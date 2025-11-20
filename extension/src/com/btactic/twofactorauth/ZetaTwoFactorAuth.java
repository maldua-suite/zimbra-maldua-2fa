/*
 * ***** BEGIN LICENSE BLOCK *****
 * Maldua Zimbra 2FA Extension
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
import java.util.List;
import java.util.Map;

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
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.btactic.twofactorauth.app.ZetaAppSpecificPassword;
import com.btactic.twofactorauth.app.ZetaAppSpecificPasswords;
import com.btactic.twofactorauth.core.BaseTwoFactorAuthComponent;
import com.btactic.twofactorauth.core.TwoFactorAuthConstants;
import com.btactic.twofactorauth.core.TwoFactorAuthUtils;
import com.btactic.twofactorauth.credentials.CredentialGenerator;
import com.btactic.twofactorauth.credentials.TOTPCredentials;
import com.btactic.twofactorauth.service.exception.SendTwoFactorAuthCodeException;
import com.btactic.twofactorauth.trusteddevices.ZetaTrustedDevices;
import com.btactic.twofactorauth.ZetaScratchCodes;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.btactic.twofactorauth.trusteddevices.ZetaTrustedDevice;
import com.btactic.twofactorauth.trusteddevices.ZetaTrustedDeviceToken;
import com.zimbra.cs.account.ldap.ChangePasswordListener;
import com.zimbra.cs.account.ldap.LdapLockoutPolicy;
import com.zimbra.cs.ldap.LdapDateUtil;

import org.apache.commons.lang.RandomStringUtils;

/**
 * This class is the main entry point for two-factor authentication.
 *
 * @author iraykin
 *
 */
public class ZetaTwoFactorAuth extends BaseTwoFactorAuthComponent implements TwoFactorAuth {
    private String secret;
    private List<String> scratchCodes;
    boolean hasStoredSecret;
    boolean hasStoredScratchCodes;
    private Map<String, ZetaAppSpecificPassword> appPasswords = new HashMap<String, ZetaAppSpecificPassword>();

    // Cached config for better performance
    private AuthenticatorConfig authenticatorConfig;

    public ZetaTwoFactorAuth(Account account) throws ServiceException {
        this(account, account.getName());
    }

    public ZetaTwoFactorAuth(Account account, String acctNamePassedIn) throws ServiceException {
        super(account, acctNamePassedIn);
        TwoFactorAuthUtils.disableTwoFactorAuthIfNecessary(account);
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

    public String loadSharedSecret() throws ServiceException {
        String encryptedSecret = account.getTwoFactorAuthSecret();
        hasStoredSecret = encryptedSecret != null;
        if (encryptedSecret != null) {
            String decrypted = decrypt(account, encryptedSecret);
            String[] parts = decrypted.split(TwoFactorAuthConstants.SECRET_SEPARATOR);
            if (parts.length != TwoFactorAuthConstants.SECRET_PARTS_COUNT) {
                throw ServiceException.FAILURE(TwoFactorAuthConstants.ERROR_INVALID_SECRET_FORMAT, null);
            }
            return parts[TwoFactorAuthConstants.SECRET_VALUE_INDEX];
        } else {
            return null;
        }
    }

    private void storeScratchCodes(List<String> codes) throws ServiceException {
        String codeString = Joiner.on(TwoFactorAuthConstants.SCRATCH_CODE_SEPARATOR).join(codes);
        String encrypted = encrypt(codeString);
        account.setTwoFactorAuthScratchCodes(encrypted);
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
        // Cache the config for better performance
        if (authenticatorConfig == null) {
            authenticatorConfig = new AuthenticatorConfig();
            String algo = getGlobalConfig().getTwoFactorAuthHashAlgorithmAsString();
            HashAlgorithm algorithm = HashAlgorithm.valueOf(algo);
            authenticatorConfig.setHashAlgorithm(algorithm);
            int codeLength = getGlobalConfig().getTwoFactorCodeLength();
            CodeLength numDigits = CodeLength.valueOf(codeLength);
            authenticatorConfig.setNumCodeDigits(numDigits);
            authenticatorConfig.setWindowSize(getGlobalConfig().getTwoFactorTimeWindowLength() / 1000);
            authenticatorConfig.allowedWindowOffset(getGlobalConfig().getTwoFactorTimeWindowOffset());
        }
        return authenticatorConfig;
    }

    /**
     * Helper class to hold parsed email 2FA code data.
     */
    private static class EmailCodeData {
        private final String code;
        private final long timestamp;

        public EmailCodeData(String code, long timestamp) {
            this.code = code;
            this.timestamp = timestamp;
        }

        public String getCode() {
            return code;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Parses encrypted email code data.
     * Eliminates code duplication across multiple methods.
     */
    private EmailCodeData parseEmailCodeData() throws ServiceException {
        String encryptedEmailData = account.getTwoFactorCodeForEmail();
        if (Strings.isNullOrEmpty(encryptedEmailData)) {
            throw AuthFailedServiceException.TWO_FACTOR_AUTH_FAILED(
                account.getName(),
                acctNamePassedIn,
                TwoFactorAuthConstants.ERROR_EMAIL_CODE_NOT_FOUND
            );
        }

        String decryptedEmailData = decrypt(account, encryptedEmailData);
        String[] parts = decryptedEmailData.split(TwoFactorAuthConstants.EMAIL_DATA_SEPARATOR);

        if (parts.length != TwoFactorAuthConstants.EMAIL_DATA_PARTS_COUNT) {
            throw ServiceException.FAILURE(TwoFactorAuthConstants.ERROR_INVALID_EMAIL_CODE_FORMAT, null);
        }

        try {
            String code = parts[TwoFactorAuthConstants.EMAIL_CODE_INDEX];
            long timestamp = Long.parseLong(parts[TwoFactorAuthConstants.EMAIL_TIMESTAMP_INDEX]);
            return new EmailCodeData(code, timestamp);
        } catch (NumberFormatException e) {
            throw ServiceException.FAILURE(TwoFactorAuthConstants.ERROR_INVALID_EMAIL_TIMESTAMP, e);
        }
    }

    private boolean checkEmailCode(String code) throws ServiceException {
        EmailCodeData emailData = parseEmailCodeData();

        long emailLifeTime = account.getTwoFactorCodeLifetimeForEmail();
        long emailExpiryTime = emailData.getTimestamp() + emailLifeTime;
        boolean emailCodeIsExpired = System.currentTimeMillis() > emailExpiryTime;

        if (emailCodeIsExpired) {
            throw SendTwoFactorAuthCodeException.CODE_EXPIRED("The email 2FA code is expired.");
        }

        return emailData.getCode().equals(code);
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

    // Is either 2FA method (app and/or email) enabled by the user?
    private boolean internalIsEnabledMethod(String twoFactorAuthMethodEnabled) throws ServiceException {
        String[] enabledMethods = account.getTwoFactorAuthMethodEnabled();
        if(Arrays.asList(enabledMethods).contains(twoFactorAuthMethodEnabled)){
            return true;
        } else {
            return false;
        }
    }

    // Is either 2FA method (app and/or email) enabled by the user?
    public boolean isEnabledMethod(String twoFactorAuthMethodEnabled) throws ServiceException {
        if (twoFactorAuthMethodEnabled == AccountConstants.E_TWO_FACTOR_METHOD_APP) {
            if (internalIsEnabledMethod(twoFactorAuthMethodEnabled)) {
                return true;
            } else {
                // Legacy fallback
                // Detect when app TwoFactorAuth was enabled
                // but there was not an specific app saved
                boolean noEnabledMethods = (this.enabledTwoFactorAuthMethodsCount() == 0) ;
                return (noEnabledMethods && account.isTwoFactorAuthEnabled());
            }
        } else {
            return internalIsEnabledMethod(twoFactorAuthMethodEnabled);
        }
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

    public void smartSetPrefPrimaryTwoFactorAuthMethod() throws ServiceException {
        // Only to be called from disableTwoFactorAuthApp and disableTwoFactorAuthEmail functions
        // We assume specific enabled attributes based on methods have been removed previously
        // Only unset if there are no remaining methods.

        if (enabledTwoFactorAuthMethodsCount() == 0) {
          account.unsetPrefPrimaryTwoFactorAuthMethod();
        } else {
          String[] enabledMethods = account.getTwoFactorAuthMethodEnabled();
          String firstEnabledMethod = enabledMethods[0];
          account.setPrefPrimaryTwoFactorAuthMethod(firstEnabledMethod);
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

    private void smartPurgeTwoFactorAuthData() throws ServiceException {
        if (enabledTwoFactorAuthMethodsCount() == 0) {
          deleteCredentials();
          ZetaAppSpecificPasswords appSpecificPasswordsManager = new ZetaAppSpecificPasswords(account);
          appSpecificPasswordsManager.revokeAll();
          account.unsetTwoFactorCodeForEmail();
        }
    }

    public void disableTwoFactorAuthApp(boolean deleteCredentials) throws ServiceException {
        checkDisableTwoFactorAuth();

        if (account.isTwoFactorAuthEnabled()) {
            account.removeTwoFactorAuthMethodEnabled(AccountConstants.E_TWO_FACTOR_METHOD_APP);
            smartUnsetZimbraTwoFactorAuthEnabled();

            smartPurgeTwoFactorAuthData();

            smartSetPrefPrimaryTwoFactorAuthMethod();
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

            smartPurgeTwoFactorAuthData();

            smartSetPrefPrimaryTwoFactorAuthMethod();
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

    public void storeEmailCode() throws ServiceException {
        int emailCodeLength = getGlobalConfig().getTwoFactorAuthEmailCodeLength();
        String emailCode = RandomStringUtils.randomNumeric(emailCodeLength);

        String reserved = ""; // Reserved for future use
        long timestamp = System.currentTimeMillis();

        String emailData = emailCode +
            TwoFactorAuthConstants.EMAIL_DATA_SEPARATOR + reserved +
            TwoFactorAuthConstants.EMAIL_DATA_SEPARATOR + timestamp;

        String encryptedEmailData = encrypt(emailData);
        account.setTwoFactorCodeForEmail(encryptedEmailData);
    }

    public String getEmailCode() throws ServiceException {
        EmailCodeData emailData = parseEmailCodeData();
        return emailData.getCode();
    }

    public long getEmailExpiryTime() throws ServiceException {
        EmailCodeData emailData = parseEmailCodeData();
        long emailLifeTime = account.getTwoFactorCodeLifetimeForEmail();
        return emailData.getTimestamp() + emailLifeTime;
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
