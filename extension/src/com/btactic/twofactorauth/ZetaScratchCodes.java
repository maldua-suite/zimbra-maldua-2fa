/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra OSE 2FA Extension
 * Copyright (C) 2023 BTACTIC, S.C.C.L.
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.btactic.twofactorauth;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.zimbra.common.auth.twofactor.AuthenticatorConfig;
import com.zimbra.common.auth.twofactor.TwoFactorOptions.CodeLength;
import com.zimbra.common.auth.twofactor.TwoFactorOptions.HashAlgorithm;
import com.zimbra.cs.account.auth.twofactor.AppSpecificPasswords;
import com.zimbra.cs.account.auth.twofactor.TrustedDevices;
import com.zimbra.cs.account.auth.twofactor.TwoFactorAuth;
import com.zimbra.cs.account.auth.twofactor.TwoFactorAuth.CredentialConfig;
import com.zimbra.cs.account.auth.twofactor.TwoFactorAuth.Factory;
import com.zimbra.cs.account.auth.twofactor.ScratchCodes;
import com.zimbra.common.auth.twofactor.TwoFactorOptions.Encoding;
import com.zimbra.common.auth.twofactor.TOTPAuthenticator;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.btactic.twofactorauth.ZetaTwoFactorAuth;
import com.btactic.twofactorauth.app.ZetaAppSpecificPassword;
import com.btactic.twofactorauth.app.ZetaAppSpecificPasswordData;
import com.btactic.twofactorauth.app.ZetaAppSpecificPasswords;
import com.btactic.twofactorauth.trusteddevices.ZetaTrustedDevices;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.btactic.twofactorauth.trusteddevices.ZetaTrustedDevice;
import com.btactic.twofactorauth.trusteddevices.ZetaTrustedDeviceToken;
import com.zimbra.cs.account.ldap.ChangePasswordListener;
import com.zimbra.cs.account.ldap.LdapLockoutPolicy;
import com.zimbra.cs.ldap.LdapDateUtil;
import com.btactic.twofactorauth.credentials.CredentialGenerator;

/**
 * This class is the main entry point for two-factor authentication.
 *
 * @author iraykin
 *
 */
public class ZetaScratchCodes implements ScratchCodes {
    private Account account;
    private String acctNamePassedIn;
    private String secret;
    private List<String> scratchCodes;
    private Encoding encoding;
    private Encoding scratchEncoding;
    boolean hasStoredSecret;
    boolean hasStoredScratchCodes;
    private Map<String, ZetaAppSpecificPassword> appPasswords = new HashMap<String, ZetaAppSpecificPassword>();

    public ZetaScratchCodes(Account account) throws ServiceException {
        this(account, account.getName());
    }

    public ZetaScratchCodes(Account account, String acctNamePassedIn) throws ServiceException {
        this.account = account;
        this.acctNamePassedIn = acctNamePassedIn;
        ZetaTwoFactorAuth manager = new ZetaTwoFactorAuth(account, acctNamePassedIn);
        manager.disableTwoFactorAuthIfNecessary();
        if (account.isFeatureTwoFactorAuthAvailable()) {
            scratchCodes = loadScratchCodes();
        }
    }

    public void clearData() throws ServiceException {
        deleteCredentials();
    }

    public CredentialConfig getCredentialConfig() throws ServiceException {
        CredentialConfig config = new CredentialConfig()
        .setSecretLength(getGlobalConfig().getTwoFactorAuthSecretLength())
        .setScratchCodeLength(getGlobalConfig().getTwoFactorScratchCodeLength())
        .setEncoding(getSecretEncoding())
        .setScratchCodeEncoding(getScratchCodeEncoding())
        .setNumScratchCodes(account.getCOS().getTwoFactorAuthNumScratchCodes());
        return config;
    }

    public AuthenticatorConfig getAuthenticatorConfig() throws ServiceException {
        AuthenticatorConfig config = new AuthenticatorConfig();
        String algo = Provisioning.getInstance().getConfig().getTwoFactorAuthHashAlgorithmAsString();
        HashAlgorithm algorithm = HashAlgorithm.valueOf(algo);
        config.setHashAlgorithm(algorithm);
        int codeLength = getGlobalConfig().getTwoFactorCodeLength();
        CodeLength numDigits = CodeLength.valueOf(codeLength);
        config.setNumCodeDigits(numDigits);
        config.setWindowSize(getGlobalConfig().getTwoFactorTimeWindowLength() / 1000);
        config.allowedWindowOffset(getGlobalConfig().getTwoFactorTimeWindowOffset());
        return config;
    }

    /* Determine if a second factor is necessary for authenticating this account */
    public boolean twoFactorAuthRequired() throws ServiceException {
        if (!account.isFeatureTwoFactorAuthAvailable()) {
            return false;
        } else {
            boolean isRequired = account.isFeatureTwoFactorAuthRequired();
            boolean isUserEnabled = account.isTwoFactorAuthEnabled();
            return isUserEnabled || isRequired;
        }
    }

    /* Determine if two-factor authentication is properly set up */
    public boolean twoFactorAuthEnabled() throws ServiceException {
        if (twoFactorAuthRequired()) {
            String secret = account.getTwoFactorAuthSecret();
            return !Strings.isNullOrEmpty(secret);
        } else {
            return false;
        }
    }

    private void storeSharedSecret(String secret) throws ServiceException {
        String encrypted = encrypt(secret);
        account.setTwoFactorAuthSecret(encrypted);
    }

    private String loadSharedSecret() throws ServiceException {
        String encryptedSecret = account.getTwoFactorAuthSecret();
        hasStoredSecret = encryptedSecret != null;
        if (encryptedSecret != null) {
            String decrypted = decrypt(account, encryptedSecret);
            String[] parts = decrypted.split("\\|");
            if (parts.length != 2) {
                throw ServiceException.FAILURE("invalid shared secret format", null);
            }
            String secret = parts[0];
            return secret;
        } else {
            return null;
        }
    }

    private static String decrypt(Account account, String encrypted) throws ServiceException {
        return DataSource.decryptData(account.getId(), encrypted);
    }

    private List<String> loadScratchCodes() throws ServiceException {
        String encryptedCodes = account.getTwoFactorAuthScratchCodes();
        if (Strings.isNullOrEmpty(encryptedCodes)) {
            hasStoredScratchCodes = false;
            return new ArrayList<String>();
        } else {
            hasStoredScratchCodes = true;
        }
        String commaSeparatedCodes = decrypt(account, encryptedCodes);
        String[] codes = commaSeparatedCodes.split(",");
        List<String> codeList = new ArrayList<String>();
        for (int i = 0; i < codes.length; i++) {
            codeList.add(codes[i]);
        }
        return codeList;
    }

    @Override
    public void storeCodes(List<String> codes) throws ServiceException {
        String codeString = Joiner.on(",").join(codes);
        String encrypted = encrypt(codeString);
        account.setTwoFactorAuthScratchCodes(encrypted);
    }

    private String encrypt(String data) throws ServiceException {
        return DataSource.encryptData(account.getId(), data);
    }

    private void storeCodes() throws ServiceException {
        if (scratchCodes != null) {
            storeCodes(scratchCodes);
        }
    }

    @Override
    public List<String> generateCodes(CredentialConfig config) throws ServiceException {
        ZimbraLog.account.debug("invalidating current scratch codes");
        List<String> newCodes = new CredentialGenerator(config).generateScratchCodes();
        scratchCodes.clear();
        scratchCodes.addAll(newCodes);
        storeCodes();
        return scratchCodes;

    }

    private Encoding getSecretEncoding() throws ServiceException {
        if (encoding == null) {
            try {
                String enc = getGlobalConfig().getTwoFactorAuthSecretEncodingAsString();
                this.encoding = Encoding.valueOf(enc);
            } catch (IllegalArgumentException e) {
                ZimbraLog.account.error("no valid shared secret encoding specified, defaulting to BASE32");
                encoding = Encoding.BASE32;
            }
        }
        return encoding;
    }

    private Encoding getScratchCodeEncoding() throws ServiceException {
        if (scratchEncoding == null) {
            try {
                String enc = getGlobalConfig().getTwoFactorAuthScratchCodeEncodingAsString();
                this.scratchEncoding = Encoding.valueOf(enc);
            } catch (IllegalArgumentException e) {
                ZimbraLog.account.error("scratch code encoding not specified, defaulting to BASE32");
                this.scratchEncoding = Encoding.BASE32;
            }
        }
        return scratchEncoding;
    }

    private Config getGlobalConfig() throws ServiceException {
        return Provisioning.getInstance().getConfig();
    }

    private boolean checkTOTPCode(String code) throws ServiceException {
        long curTime = System.currentTimeMillis() / 1000;
        AuthenticatorConfig config = getAuthenticatorConfig();
        TOTPAuthenticator auth = new TOTPAuthenticator(config);
        return auth.validateCode(secret, curTime, code, getSecretEncoding());
    }

    public void authenticate(String scratchCode) throws ServiceException {
        if (!checkScratchCodes(scratchCode)) {
            failedLogin();
            ZimbraLog.account.error("invalid scratch code");
            throw AuthFailedServiceException.TWO_FACTOR_AUTH_FAILED(account.getName(), acctNamePassedIn, "invalid scratch code");
        }
    }

    public boolean checkScratchCodes(String scratchCode) throws ServiceException {
        for (String code: scratchCodes) {
            if (code.equals(scratchCode)) {
                invalidateScratchCode(code);
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> getCodes() {
        return scratchCodes;
    }

    public List<String> generateNewScratchCodes() throws ServiceException {
        ZimbraLog.account.debug("invalidating current scratch codes");
        CredentialConfig config = getCredentialConfig();
        List<String> newCodes = new CredentialGenerator(config).generateScratchCodes();
        scratchCodes.clear();
        scratchCodes.addAll(newCodes);
        storeScratchCodes();
        return scratchCodes;

    }

    private void storeScratchCodes(List<String> codes) throws ServiceException {
        String codeString = Joiner.on(",").join(codes);
        String encrypted = encrypt(codeString);
        account.setTwoFactorAuthScratchCodes(encrypted);
    }

    private void storeScratchCodes() throws ServiceException {
        if (scratchCodes != null) {
            storeScratchCodes(scratchCodes);
        }
    }

    private void invalidateScratchCode(String code) throws ServiceException {
        scratchCodes.remove(code);
        storeCodes();
    }

    public void deleteCredentials() throws ServiceException {
        account.setTwoFactorAuthScratchCodes(null);
    }

    private void failedLogin() throws ServiceException {
        LdapLockoutPolicy lockoutPolicy = new LdapLockoutPolicy(Provisioning.getInstance(), account);
        lockoutPolicy.failedSecondFactorLogin();
    }

}
