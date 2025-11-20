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

import java.util.Date;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.ldap.LdapDateUtil;

/**
 * Utility methods for two-factor authentication operations.
 * This class provides static helper methods used across multiple 2FA components.
 *
 * @author BTACTIC
 */
public final class TwoFactorAuthUtils {

    private TwoFactorAuthUtils() {
        // Prevent instantiation of utility class
        throw new AssertionError("Cannot instantiate utility class");
    }

    /**
     * Disables two-factor authentication if the last reset date is after
     * the timestamp of the stored secret. This handles the case where an
     * admin has reset 2FA globally.
     *
     * @param account the account to check
     * @throws ServiceException if the operation fails
     */
    public static void disableTwoFactorAuthIfNecessary(Account account) throws ServiceException {
        String encryptedSecret = account.getTwoFactorAuthSecret();
        if (Strings.isNullOrEmpty(encryptedSecret)) {
            return;
        }

        String decrypted = DataSource.decryptData(account.getId(), encryptedSecret);
        String[] parts = decrypted.split(TwoFactorAuthConstants.SECRET_SEPARATOR);

        Date timestamp;
        if (parts.length == TwoFactorAuthConstants.SECRET_PARTS_COUNT_LEGACY) {
            // For backwards compatibility with the server version
            // that did not store a timestamp.
            timestamp = null;
        } else if (parts.length == TwoFactorAuthConstants.SECRET_PARTS_COUNT) {
            try {
                timestamp = LdapDateUtil.parseGeneralizedTime(
                    parts[TwoFactorAuthConstants.SECRET_TIMESTAMP_INDEX]
                );
            } catch (NumberFormatException e) {
                throw ServiceException.FAILURE(
                    TwoFactorAuthConstants.ERROR_INVALID_SECRET_TIMESTAMP, e
                );
            }
        } else {
            throw ServiceException.FAILURE(
                TwoFactorAuthConstants.ERROR_INVALID_SECRET_FORMAT, null
            );
        }

        Date lastDisabledDate = account.getCOS().getTwoFactorAuthLastReset();
        if (lastDisabledDate == null) {
            return;
        }

        if (timestamp == null || lastDisabledDate.after(timestamp)) {
            clearTwoFactorAuthData(account);
        }
    }

    /**
     * Clears all 2FA data for an account.
     * This is a complete reset of 2FA including credentials, scratch codes,
     * app-specific passwords, and trusted devices.
     *
     * @param account the account to clear
     * @throws ServiceException if the operation fails
     */
    private static void clearTwoFactorAuthData(Account account) throws ServiceException {
        account.setTwoFactorAuthEnabled(false);
        account.setTwoFactorAuthSecret(null);
        account.setTwoFactorAuthScratchCodes(null);

        // Clear app-specific passwords
        String[] passwords = account.getAppSpecificPassword();
        for (String password : passwords) {
            account.removeAppSpecificPassword(password);
        }

        // Clear trusted devices
        String[] trustedDevices = account.getTwoFactorAuthTrustedDevices();
        for (String device : trustedDevices) {
            account.removeTwoFactorAuthTrustedDevices(device);
        }
    }
}
