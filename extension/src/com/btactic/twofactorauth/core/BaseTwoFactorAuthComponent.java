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
package com.btactic.twofactorauth.core;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.auth.twofactor.TwoFactorOptions.Encoding;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

/**
 * Base class for all 2FA components providing common functionality.
 * This eliminates code duplication across ZetaTwoFactorAuth,
 * ZetaScratchCodes, ZetaAppSpecificPasswords, and ZetaTrustedDevices.
 *
 * @author BTACTIC
 */
public abstract class BaseTwoFactorAuthComponent {
    protected final Account account;
    protected final String acctNamePassedIn;

    // Cached config objects to improve performance
    private Config globalConfig;
    private Encoding secretEncoding;
    private Encoding scratchEncoding;

    /**
     * Creates a new component for the given account.
     *
     * @param account the account to manage
     * @throws ServiceException if initialization fails
     * @throws IllegalArgumentException if account is null
     */
    protected BaseTwoFactorAuthComponent(Account account) throws ServiceException {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        this(account, account.getName());
    }

    /**
     * Creates a new component for the given account with specific name.
     *
     * @param account the account to manage
     * @param acctNamePassedIn the account name passed in
     * @throws ServiceException if initialization fails
     * @throws IllegalArgumentException if account is null or acctNamePassedIn is empty
     */
    protected BaseTwoFactorAuthComponent(Account account, String acctNamePassedIn)
            throws ServiceException {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        if (acctNamePassedIn == null || acctNamePassedIn.trim().isEmpty()) {
            throw new IllegalArgumentException("Account name cannot be null or empty");
        }

        this.account = account;
        this.acctNamePassedIn = acctNamePassedIn;
    }

    /**
     * Gets global configuration with caching for better performance.
     * The config is loaded once and reused for subsequent calls.
     *
     * @return the global Zimbra configuration
     * @throws ServiceException if config cannot be retrieved
     */
    protected Config getGlobalConfig() throws ServiceException {
        if (globalConfig == null) {
            globalConfig = Provisioning.getInstance().getConfig();
        }
        return globalConfig;
    }

    /**
     * Gets secret encoding with caching.
     * Falls back to BASE32 if the configured encoding is invalid.
     *
     * @return the secret encoding scheme
     * @throws ServiceException if config cannot be retrieved
     */
    protected Encoding getSecretEncoding() throws ServiceException {
        if (secretEncoding == null) {
            try {
                String enc = getGlobalConfig().getTwoFactorAuthSecretEncodingAsString();
                secretEncoding = Encoding.valueOf(enc);
            } catch (IllegalArgumentException e) {
                ZimbraLog.account.warn("Invalid secret encoding, defaulting to BASE32", e);
                secretEncoding = Encoding.BASE32;
            }
        }
        return secretEncoding;
    }

    /**
     * Gets scratch code encoding with caching.
     * Falls back to BASE32 if the configured encoding is invalid.
     *
     * @return the scratch code encoding scheme
     * @throws ServiceException if config cannot be retrieved
     */
    protected Encoding getScratchCodeEncoding() throws ServiceException {
        if (scratchEncoding == null) {
            try {
                String enc = getGlobalConfig().getTwoFactorAuthScratchCodeEncodingAsString();
                scratchEncoding = Encoding.valueOf(enc);
            } catch (IllegalArgumentException e) {
                ZimbraLog.account.warn("Invalid scratch code encoding, defaulting to BASE32", e);
                scratchEncoding = Encoding.BASE32;
            }
        }
        return scratchEncoding;
    }

    /**
     * Encrypts data using account-specific encryption.
     *
     * @param data the data to encrypt
     * @return the encrypted data
     * @throws ServiceException if encryption fails
     */
    protected String encrypt(String data) throws ServiceException {
        return DataSource.encryptData(account.getId(), data);
    }

    /**
     * Decrypts data using account-specific decryption.
     *
     * @param account the account whose key to use
     * @param encrypted the encrypted data
     * @return the decrypted data
     * @throws ServiceException if decryption fails
     */
    protected static String decrypt(Account account, String encrypted)
            throws ServiceException {
        return DataSource.decryptData(account.getId(), encrypted);
    }

    /**
     * Determines if 2FA is required for this account.
     * 2FA is required if the feature is available AND (enabled by user OR required by admin).
     *
     * @return true if 2FA is required
     * @throws ServiceException if account attributes cannot be read
     */
    public boolean twoFactorAuthRequired() throws ServiceException {
        if (!account.isFeatureTwoFactorAuthAvailable()) {
            return false;
        }
        return account.isTwoFactorAuthEnabled() ||
               account.isFeatureTwoFactorAuthRequired();
    }

    /**
     * Clears all data for this component.
     * Subclasses must implement this to clean up their specific data.
     *
     * @throws ServiceException if cleanup fails
     */
    public abstract void clearData() throws ServiceException;
}
