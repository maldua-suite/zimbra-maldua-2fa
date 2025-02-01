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

import java.util.Arrays;
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
import com.zimbra.cs.service.util.JWEUtil;
import com.zimbra.common.auth.twofactor.TwoFactorOptions.Encoding;
import com.zimbra.common.auth.twofactor.TOTPAuthenticator;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.btactic.twofactorauth.app.ZetaAppSpecificPassword;
import com.btactic.twofactorauth.app.ZetaAppSpecificPasswordData;
import com.btactic.twofactorauth.app.ZetaAppSpecificPasswords;
import com.btactic.twofactorauth.credentials.CredentialGenerator;
import com.btactic.twofactorauth.credentials.TOTPCredentials;
import com.btactic.twofactorauth.trusteddevices.ZetaTrustedDevices;
import com.btactic.twofactorauth.ZetaScratchCodes;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.btactic.twofactorauth.trusteddevices.ZetaTrustedDevice;
import com.btactic.twofactorauth.trusteddevices.ZetaTrustedDeviceToken;
import com.zimbra.cs.account.ldap.ChangePasswordListener;
import com.zimbra.cs.account.ldap.LdapLockoutPolicy;
import com.zimbra.cs.ldap.LdapDateUtil;

/**
 * This class is the main entry point for two-factor authentication.
 *
 * @author iraykin
 *
 */
public class ZetaTwoFactorAuth extends TwoFactorAuth {
    private Account account;
    private String acctNamePassedIn;
    private String secret;
    private List<String> scratchCodes;
    private Encoding encoding;
    private Encoding scratchEncoding;
    boolean hasStoredSecret;
    boolean hasStoredScratchCodes;
    private Map<String, ZetaAppSpecificPassword> appPasswords = new HashMap<String, ZetaAppSpecificPassword>();

    public ZetaTwoFactorAuth(Account account) throws ServiceException {
        this(account, account.getName());
    }

    public ZetaTwoFactorAuth(Account account, String acctNamePassedIn) throws ServiceException {
        super(account, acctNamePassedIn);
        this.account = account;
        this.acctNamePassedIn = acctNamePassedIn;
        disableTwoFactorAuthIfNecessary();
        if (account.isFeatureTwoFactorAuthAvailable()) {
            secret = loadSharedSecret();
        }
    }

    public static class AuthFactory implements Factory {

        @Override
        public TwoFactorAuth getTwoFactorAuth(Account account, String acctNamePassedIn) throws ServiceException {
            return new ZetaTwoFactorAuth(account, acctNamePassedIn);
        }

        @Override
        public TwoFactorAuth getTwoFactorAuth(Account account) throws ServiceException {
            return new ZetaTwoFactorAuth(account);
        }

        @Override
        public TrustedDevices getTrustedDevices(Account account) throws ServiceException {
            return new ZetaTrustedDevices(account);
        }

        @Override
        public TrustedDevices getTrustedDevices(Account account, String acctNamePassedIn) throws ServiceException {
            return new ZetaTrustedDevices(account, acctNamePassedIn);
        }

        @Override
        public AppSpecificPasswords getAppSpecificPasswords(Account account) throws ServiceException {
            return new ZetaAppSpecificPasswords(account);
        }

        @Override
        public AppSpecificPasswords getAppSpecificPasswords(Account account, String acctNamePassedIn) throws ServiceException {
            return new ZetaAppSpecificPasswords(account, acctNamePassedIn);
        }

        @Override
        public ScratchCodes getScratchCodes(Account account) throws ServiceException {
            return new ZetaScratchCodes(account);
        }

        @Override
        public ScratchCodes getScratchCodes(Account account, String acctNamePassedIn) throws ServiceException {
            return new ZetaScratchCodes(account, acctNamePassedIn);
        }

    }

    public void disableTwoFactorAuthIfNecessary() throws ServiceException {
        String encryptedSecret = account.getTwoFactorAuthSecret();
        if (!Strings.isNullOrEmpty(encryptedSecret)) {
            String decrypted = decrypt(account, encryptedSecret);
            String[] parts = decrypted.split("\\|");
            Date timestamp;
            if (parts.length == 1) {
                // For backwards compatability with the server version
                // that did not store a timestamp.
                timestamp = null;
            } else if (parts.length > 2) {
                throw ServiceException.FAILURE("invalid shared secret format", null);
            }
            try {
                timestamp = LdapDateUtil.parseGeneralizedTime(parts[1]);
            } catch (NumberFormatException e) {
                throw ServiceException.FAILURE("invalid shared secret timestamp", null);
            }
            Date lastDisabledDate = account.getCOS().getTwoFactorAuthLastReset();
            if (lastDisabledDate == null) {
                return;
            }
            if (timestamp == null || lastDisabledDate.after(timestamp)) {
                clearData();
            }
        }
    }

    public void clear2FAData() throws ServiceException {
        account.setTwoFactorAuthEnabled(false);
        delete2FACredentials();
    }

    @Override
    public void clearData() throws ServiceException {
        clear2FAData();
        ZetaScratchCodes scratchCodesManager = new ZetaScratchCodes(account);
        scratchCodesManager.clearData();
        ZetaAppSpecificPasswords appSpecificPasswordsManager = new ZetaAppSpecificPasswords(account);
        appSpecificPasswordsManager.clearData();
        ZetaTrustedDevices trustedDevicesManager = new ZetaTrustedDevices(account);
        trustedDevicesManager.clearData();
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

    private void storeScratchCodes(List<String> codes) throws ServiceException {
        String codeString = Joiner.on(",").join(codes);
        String encrypted = encrypt(codeString);
        account.setTwoFactorAuthScratchCodes(encrypted);
    }

    private String encrypt(String data) throws ServiceException {
        return DataSource.encryptData(account.getId(), data);
    }

    private void storeScratchCodes() throws ServiceException {
        if (scratchCodes != null) {
            storeScratchCodes(scratchCodes);
        }
    }


    public TOTPCredentials generateNewCredentials() throws ServiceException {
        CredentialConfig config = getCredentialConfig();
        TOTPCredentials credentials = new CredentialGenerator(config).generateCredentials();
        return credentials;
    }

    private void storeCredentials(TOTPCredentials credentials) throws ServiceException {
        String secret = String.format("%s|%s", credentials.getSecret(), credentials.getTimestamp());
        storeSharedSecret(secret);
        storeScratchCodes(credentials.getScratchCodes());
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

    @Override
    public CredentialConfig getCredentialConfig() throws ServiceException {
        CredentialConfig config = new CredentialConfig()
        .setSecretLength(getGlobalConfig().getTwoFactorAuthSecretLength())
        .setScratchCodeLength(getGlobalConfig().getTwoFactorScratchCodeLength())
        .setEncoding(getSecretEncoding())
        .setScratchCodeEncoding(getScratchCodeEncoding())
        .setNumScratchCodes(account.getCOS().getTwoFactorAuthNumScratchCodes());
        return config;
    }

    @Override
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

    private boolean checkEmailCode(String code) throws ServiceException {
        String encryptedEmailData = account.getTwoFactorCodeForEmail();
        if (encryptedEmailData == null || encryptedEmailData.isEmpty()) {
            throw AuthFailedServiceException.TWO_FACTOR_AUTH_FAILED(account.getName(), acctNamePassedIn, "Email based 2FA code not found on server.");
        }
        String decryptedEmailData = decrypt(account, encryptedEmailData);

        String[] parts = decryptedEmailData.split(":");
        if (parts.length != 3) {
            throw ServiceException.FAILURE("invalid email code format", null);
        }
        String emailCode = parts[0];
        String unKnownData2 = parts[1];
        String timestamp = parts[2];
        // Decryption example:
        // decryptedEmailData: '6912720::1738424806645'
        // emailCode :    '6912720'
        // unKnownData2 : ''
        // timestamp:     '1738424806645'

        // TODO: Either return false or throw exception when the code is expired
        // based on saved timestamp + email code expiry time (Read from config)

        ZimbraLog.account.error("MALDUA-DEBUG EmailTwoFactorCode: '" + decryptedEmailData + "'");
        return (emailCode.equals(code));
    }

    private boolean checkTOTPCode(String code) throws ServiceException {
        long curTime = System.currentTimeMillis() / 1000;
        AuthenticatorConfig config = getAuthenticatorConfig();
        TOTPAuthenticator auth = new TOTPAuthenticator(config);
        return auth.validateCode(secret, curTime, code, getSecretEncoding());
    }

    private boolean isEmailCode(String code) throws ServiceException {
      int emailCodeLength = getGlobalConfig().getTwoFactorAuthEmailCodeLength();
      return code.length() == emailCodeLength;
    }

    private Boolean isScratchCode(String code) throws ServiceException {
      int scratchCodeLength = getGlobalConfig().getTwoFactorScratchCodeLength();
      return code.length() == scratchCodeLength;
    }

    private Boolean isTOTPCode(String code) throws ServiceException {
      int totpLength = getGlobalConfig().getTwoFactorCodeLength();
      return code.length() == totpLength;
    }

    @Override
    public void authenticateTOTP(String code) throws ServiceException {
        if (!checkTOTPCode(code)) {
            ZimbraLog.account.error("invalid TOTP code");
            throw AuthFailedServiceException.TWO_FACTOR_AUTH_FAILED(account.getName(), acctNamePassedIn, "invalid TOTP code");
        }
    }

    @Override
    public void authenticate(String code) throws ServiceException {
        if (code == null) {
            ZimbraLog.account.error("two-factor code missing");
            throw AuthFailedServiceException.TWO_FACTOR_AUTH_FAILED(account.getName(), acctNamePassedIn, "two-factor code missing");
        }

        boolean success = false;

        if (isTOTPCode(code)) {
          success = checkTOTPCode(code);
        } else if (isEmailCode(code)) {
          success = checkEmailCode(code);
        } else if (isScratchCode(code)) {
          ZetaScratchCodes scratchCodesManager = new ZetaScratchCodes(account);
          success = scratchCodesManager.checkScratchCodes(code);
        }

        if (!success) {
            failedLogin();
            ZimbraLog.account.error("invalid two-factor code");
            throw AuthFailedServiceException.TWO_FACTOR_AUTH_FAILED(account.getName(), acctNamePassedIn, "invalid two-factor code");
        }
    }

    @Override
    public TOTPCredentials generateCredentials() throws ServiceException {
        if (!account.isTwoFactorAuthEnabled()) {
            TOTPCredentials creds = generateNewCredentials();
            storeCredentials(creds);
            return creds;
        } else {
            ZimbraLog.account.info("two-factor authentication already enabled");
            return null;
        }
    }

    @Override
    public void enableTwoFactorAuth() throws ServiceException {
        account.setTwoFactorAuthEnabled(true);
    }

    // What 2FA method is enabled by user: app and/or email
    public void addEnabledMethod(String twoFactorAuthMethodEnabled) throws ServiceException {
        account.addTwoFactorAuthMethodEnabled(twoFactorAuthMethodEnabled);
    }

    // Is either 2FA method (app and/or email) allowed to an user to use?
    public boolean isAllowedMethod(String twoFactorAuthMethodAllowed) throws ServiceException {
        String[] allowedMethods = account.getTwoFactorAuthMethodAllowed();
        if(Arrays.asList(allowedMethods).contains(twoFactorAuthMethodAllowed)){
            return true;
        } else {
            return false;
        }
    }

    // Count 2FA (app and/or email) enabled methods.
    private int enabledTwoFactorAuthMethodsCount() throws ServiceException {
        String[] enabledMethods = account.getTwoFactorAuthMethodEnabled();
        return (enabledMethods.length);
    }

    private void delete2FACredentials() throws ServiceException {
        account.setTwoFactorAuthSecret(null);
    }

    private void deleteCredentials() throws ServiceException {
        delete2FACredentials();
        ZetaScratchCodes scratchCodesManager = new ZetaScratchCodes(account);
        scratchCodesManager.deleteCredentials();
    }

    public void smartUnsetZimbraTwoFactorAuthEnabled() throws ServiceException {
        // We assume specific enabled attributes based on methods have been removed previously
        // Only unset if there are no remaining methods.

        if (enabledTwoFactorAuthMethodsCount() == 0) {
          if (account.isTwoFactorAuthEnabled()) {
              account.setTwoFactorAuthEnabled(false);
          } else {
              ZimbraLog.account.info("two-factor authentication already disabled");
          }
        }
    }

    public void checkDisableTwoFactorAuth() throws ServiceException {
        // Option 1: Two methods enabled: OK
        // Option 2: If only one method enabled then only disable if not required
        if (enabledTwoFactorAuthMethodsCount() == 1) {
          if (account.isFeatureTwoFactorAuthRequired()) {
              throw ServiceException.CANNOT_DISABLE_TWO_FACTOR_AUTH();
          }
        }
    }

    public void disableTwoFactorAuthApp(boolean deleteCredentials) throws ServiceException {
        checkDisableTwoFactorAuth();

        if (account.isTwoFactorAuthEnabled()) {
            account.removeTwoFactorAuthMethodEnabled(AccountConstants.E_TWO_FACTOR_METHOD_APP);
            smartUnsetZimbraTwoFactorAuthEnabled();
            if (deleteCredentials) {
                deleteCredentials();
            }
            ZetaAppSpecificPasswords appSpecificPasswordsManager = new ZetaAppSpecificPasswords(account);
            appSpecificPasswordsManager.revokeAll();
        } else {
            ZimbraLog.account.info("two-factor authentication already disabled");
        }
    }

    public void disableTwoFactorAuthEmail() throws ServiceException {
        checkDisableTwoFactorAuth();

        if (account.isTwoFactorAuthEnabled()) {
            account.removeTwoFactorAuthMethodEnabled(AccountConstants.E_TWO_FACTOR_METHOD_EMAIL);
            smartUnsetZimbraTwoFactorAuthEnabled();
            account.unsetPrefPasswordRecoveryAddress();
            account.unsetPrefPasswordRecoveryAddressStatus();
        } else {
            ZimbraLog.account.info("two-factor authentication already disabled");
        }
    }

    @Override
    public void disableTwoFactorAuth(boolean deleteCredentials) throws ServiceException {
        if (account.isFeatureTwoFactorAuthRequired()) {
            throw ServiceException.CANNOT_DISABLE_TWO_FACTOR_AUTH();
        } else if (account.isTwoFactorAuthEnabled()) {
            account.setTwoFactorAuthEnabled(false);
            if (deleteCredentials) {
                deleteCredentials();
            }
            ZetaAppSpecificPasswords appSpecificPasswordsManager = new ZetaAppSpecificPasswords(account);
            appSpecificPasswordsManager.revokeAll();
        } else {
            ZimbraLog.account.info("two-factor authentication already disabled");
        }
    }

    public List<ZetaTrustedDevice> getTrustedDevices() throws ServiceException {
        List<ZetaTrustedDevice> trustedDevices = new ArrayList<ZetaTrustedDevice>();
        for (String encoded: account.getTwoFactorAuthTrustedDevices()) {
            try {
                ZetaTrustedDevice td = new ZetaTrustedDevice(account, encoded);
                if (td.isExpired()) {
                    td.revoke();
                }
                trustedDevices.add(td);
            } catch (ServiceException e) {
                ZimbraLog.account.error(e.getMessage());
                account.removeTwoFactorAuthTrustedDevices(encoded);
            }
        }
        return trustedDevices;
    }

    public void revokeAllTrustedDevices() throws ServiceException {
        ZimbraLog.account.debug("revoking all trusted devices");
        for (ZetaTrustedDevice td: getTrustedDevices()) {
            td.revoke();
        }
    }

    private void failedLogin() throws ServiceException {
        LdapLockoutPolicy lockoutPolicy = new LdapLockoutPolicy(Provisioning.getInstance(), account);
        lockoutPolicy.failedSecondFactorLogin();
    }

    public static class TwoFactorPasswordChange extends ChangePasswordListener {
        public static final String LISTENER_NAME = "twofactorpasswordchange";

        @Override
        public void preModify(Account acct, String newPassword, Map context,
                Map<String, Object> attrsToModify) throws ServiceException {
        }

        @Override
        public void postModify(Account acct, String newPassword, Map context) {
            if (acct.isRevokeAppSpecificPasswordsOnPasswordChange()) {
                try {
                    ZimbraLog.account.info("revoking all app-specific passwords due to password change");
                    new ZetaAppSpecificPasswords(acct).revokeAll();
                } catch (ServiceException e) {
                    ZimbraLog.account.error("could not revoke app-specific passwords on password change", e);
                }
            }
        }
    }
}
