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
import com.btactic.twofactorauth.core.EmailCodeParser;
import com.btactic.twofactorauth.core.TwoFactorAuthConstants;
import com.btactic.twofactorauth.core.TwoFactorAuthUtils;
import com.btactic.twofactorauth.credentials.CredentialGenerator;
import com.btactic.twofactorauth.credentials.TOTPCredentials;
import com.btactic.twofactorauth.exception.TwoFactorCodeExpiredException;
import com.btactic.twofactorauth.exception.TwoFactorCodeInvalidException;
import com.btactic.twofactorauth.exception.TwoFactorCredentialException;
import com.btactic.twofactorauth.exception.TwoFactorCredentialException.CredentialErrorType;
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
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        this(account, account.getName());
    }

    public ZetaTwoFactorAuth(Account account, String acctNamePassedIn) throws ServiceException {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        if (Strings.isNullOrEmpty(acctNamePassedIn)) {
            throw new IllegalArgumentException("Account name cannot be null or empty");
        }

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

    /**
     * Determines if two-factor authentication is required for this account.
     * 2FA is required if the feature is available AND either:
     * - The user has explicitly enabled it, OR
     * - The admin has made it mandatory for the account
     *
     * @return true if 2FA is required for authentication
     * @throws ServiceException if account attributes cannot be read
     */
    public boolean twoFactorAuthRequired() throws ServiceException {
        if (!account.isFeatureTwoFactorAuthAvailable()) {
            return false;
        } else {
            boolean isRequired = account.isFeatureTwoFactorAuthRequired();
            boolean isUserEnabled = account.isTwoFactorAuthEnabled();
            return isUserEnabled || isRequired;
        }
    }

    /**
     * Determines if two-factor authentication is properly configured and set up.
     * This checks both that 2FA is required AND that credentials have been generated.
     *
     * @return true if 2FA is enabled and configured with valid credentials
     * @throws ServiceException if account attributes cannot be read
     */
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
                throw new TwoFactorCredentialException(
                    TwoFactorAuthConstants.ERROR_INVALID_SECRET_FORMAT,
                    account.getName(),
                    acctNamePassedIn,
                    "shared secret",
                    CredentialErrorType.INVALID_FORMAT
                );
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
     * Checks if the provided email code is valid.
     * Uses EmailCodeParser to validate the code and check expiration.
     *
     * @param code the email code provided by the user
     * @return true if the code is valid and not expired
     * @throws ServiceException if validation fails or code has expired
     */
    private boolean checkEmailCode(String code) throws ServiceException {
        long emailLifeTime = account.getTwoFactorCodeLifetimeForEmail();
        // EmailCodeParser.validateAndParse throws exception if invalid or expired
        EmailCodeParser.validateAndParse(account, acctNamePassedIn, code, emailLifeTime);
        return true;
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
            ZimbraLog.account.error("invalid TOTP code for account: " + account.getName());
            throw new TwoFactorCodeInvalidException(
                account.getName(),
                acctNamePassedIn,
                "TOTP",
                "code does not match expected value"
            );
        }
    }

    @Override
    public void authenticate(String code) throws ServiceException {
        if (code == null) {
            ZimbraLog.account.error("2FA authentication failed for account " + account.getName() + ": code missing");
            throw new TwoFactorCodeInvalidException(
                account.getName(),
                acctNamePassedIn,
                "unknown",
                "code is null or missing"
            );
        }

        boolean success = false;
        String codeType = "unknown";

        if (isTOTPCode(code)) {
          codeType = "TOTP";
          success = checkTOTPCode(code);
        } else if (isEmailCode(code)) {
          codeType = "Email";
          success = checkEmailCode(code);
        } else if (isScratchCode(code)) {
          codeType = "Scratch";
          ZetaScratchCodes scratchCodesManager = new ZetaScratchCodes(account);
          success = scratchCodesManager.checkScratchCodes(code);
        }

        if (!success) {
            failedLogin();
            ZimbraLog.account.error("2FA authentication failed for account " + account.getName() + ": invalid " + codeType + " code");
            throw new TwoFactorCodeInvalidException(
                account.getName(),
                acctNamePassedIn,
                codeType,
                "code does not match expected value"
            );
        }

        ZimbraLog.account.info("2FA authentication successful for account " + account.getName() + " using " + codeType + " code");
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
        ZimbraLog.account.info("Enabling 2FA for account: " + account.getName());
        account.setTwoFactorAuthEnabled(true);
    }

    // What 2FA method is enabled by user: app and/or email
    public void addEnabledMethod(String twoFactorAuthMethodEnabled) throws ServiceException {
        account.addTwoFactorAuthMethodEnabled(twoFactorAuthMethodEnabled);
    }

    /**
     * Internal method to check if a 2FA method is enabled.
     * Optimized to avoid creating ArrayList for each check.
     *
     * @param twoFactorAuthMethodEnabled the method to check
     * @return true if the method is enabled
     * @throws ServiceException if account attributes cannot be read
     */
    private boolean internalIsEnabledMethod(String twoFactorAuthMethodEnabled) throws ServiceException {
        String[] enabledMethods = account.getTwoFactorAuthMethodEnabled();
        // Direct array iteration is more efficient than Arrays.asList().contains()
        for (String method : enabledMethods) {
            if (method.equals(twoFactorAuthMethodEnabled)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a specific 2FA method is enabled by the user.
     * Includes legacy fallback for app-based 2FA.
     *
     * @param twoFactorAuthMethodEnabled the method to check (e.g., "app", "email")
     * @return true if the method is enabled
     * @throws ServiceException if account attributes cannot be read
     */
    public boolean isEnabledMethod(String twoFactorAuthMethodEnabled) throws ServiceException {
        if (twoFactorAuthMethodEnabled == AccountConstants.E_TWO_FACTOR_METHOD_APP) {
            if (internalIsEnabledMethod(twoFactorAuthMethodEnabled)) {
                return true;
            } else {
                // Legacy fallback: detect when app TwoFactorAuth was enabled
                // but there was not a specific app method saved
                int methodCount = enabledTwoFactorAuthMethodsCount();
                return (methodCount == 0 && account.isTwoFactorAuthEnabled());
            }
        } else {
            return internalIsEnabledMethod(twoFactorAuthMethodEnabled);
        }
    }

    /**
     * Checks if a specific 2FA method is allowed for the user.
     * Optimized to avoid creating ArrayList for each check.
     *
     * @param twoFactorAuthMethodAllowed the method to check (e.g., "app", "email")
     * @return true if the method is allowed
     * @throws ServiceException if account attributes cannot be read
     */
    public boolean isAllowedMethod(String twoFactorAuthMethodAllowed) throws ServiceException {
        String[] allowedMethods = account.getTwoFactorAuthMethodAllowed();
        // Direct array iteration is more efficient than Arrays.asList().contains()
        for (String method : allowedMethods) {
            if (method.equals(twoFactorAuthMethodAllowed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counts the number of enabled 2FA methods for this account.
     *
     * @return the count of enabled methods
     * @throws ServiceException if account attributes cannot be read
     */
    private int enabledTwoFactorAuthMethodsCount() throws ServiceException {
        String[] enabledMethods = account.getTwoFactorAuthMethodEnabled();
        return enabledMethods.length;
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
        ZimbraLog.account.info("Disabling app-based 2FA for account: " + account.getName());
        checkDisableTwoFactorAuth();

        if (account.isTwoFactorAuthEnabled()) {
            account.removeTwoFactorAuthMethodEnabled(AccountConstants.E_TWO_FACTOR_METHOD_APP);
            smartUnsetZimbraTwoFactorAuthEnabled();

            smartPurgeTwoFactorAuthData();

            smartSetPrefPrimaryTwoFactorAuthMethod();
            ZimbraLog.account.info("Successfully disabled app-based 2FA for account: " + account.getName());
        } else {
            ZimbraLog.account.info("two-factor authentication already disabled for account: " + account.getName());
        }
    }

    public void disableTwoFactorAuthEmail() throws ServiceException {
        ZimbraLog.account.info("Disabling email-based 2FA for account: " + account.getName());
        checkDisableTwoFactorAuth();

        if (account.isTwoFactorAuthEnabled()) {
            account.removeTwoFactorAuthMethodEnabled(AccountConstants.E_TWO_FACTOR_METHOD_EMAIL);
            smartUnsetZimbraTwoFactorAuthEnabled();
            account.unsetPrefPasswordRecoveryAddress();
            account.unsetPrefPasswordRecoveryAddressStatus();

            smartPurgeTwoFactorAuthData();

            smartSetPrefPrimaryTwoFactorAuthMethod();
            ZimbraLog.account.info("Successfully disabled email-based 2FA for account: " + account.getName());
        } else {
            ZimbraLog.account.info("two-factor authentication already disabled for account: " + account.getName());
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
