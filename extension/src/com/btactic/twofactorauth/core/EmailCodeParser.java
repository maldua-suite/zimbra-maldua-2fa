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

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.btactic.twofactorauth.exception.TwoFactorCredentialException;
import com.btactic.twofactorauth.exception.TwoFactorCredentialException.CredentialErrorType;

/**
 * Utility class for parsing and validating email-based 2FA codes.
 * This class centralizes the logic for extracting, decrypting, and parsing
 * email verification codes stored in account attributes.
 *
 * <p>Email code format: "code:reserved:timestamp"
 * <ul>
 *   <li>code: The actual verification code sent to the user's email</li>
 *   <li>reserved: Reserved field for future use (currently unused)</li>
 *   <li>timestamp: Unix timestamp in milliseconds when the code was generated</li>
 * </ul>
 *
 * @author BTACTIC
 */
public final class EmailCodeParser {

    /**
     * Represents parsed email code data with code value and generation timestamp.
     */
    public static class EmailCodeData {
        private final String code;
        private final long timestamp;

        /**
         * Creates new email code data.
         *
         * @param code the verification code
         * @param timestamp the generation timestamp in milliseconds
         */
        public EmailCodeData(String code, long timestamp) {
            this.code = code;
            this.timestamp = timestamp;
        }

        /**
         * Gets the verification code.
         *
         * @return the code
         */
        public String getCode() {
            return code;
        }

        /**
         * Gets the generation timestamp.
         *
         * @return the timestamp in milliseconds since epoch
         */
        public long getTimestamp() {
            return timestamp;
        }

        /**
         * Checks if the code has expired based on the given lifetime.
         *
         * @param lifetimeMs the maximum lifetime of the code in milliseconds
         * @return true if the code has expired
         */
        public boolean isExpired(long lifetimeMs) {
            long expiryTime = timestamp + lifetimeMs;
            return System.currentTimeMillis() > expiryTime;
        }

        /**
         * Gets the expiration time of this code.
         *
         * @param lifetimeMs the maximum lifetime of the code in milliseconds
         * @return the expiration timestamp in milliseconds since epoch
         */
        public long getExpiryTime(long lifetimeMs) {
            return timestamp + lifetimeMs;
        }
    }

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private EmailCodeParser() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    /**
     * Parses email code data from an account.
     * Retrieves, decrypts, and parses the email verification code stored in the account.
     *
     * @param account the account to retrieve the code from
     * @param acctNamePassedIn the account name as passed in by the user
     * @return parsed email code data
     * @throws ServiceException if the code is missing, malformed, or cannot be parsed
     */
    public static EmailCodeData parse(Account account, String acctNamePassedIn)
            throws ServiceException {
        // Retrieve encrypted email code from account
        String encryptedEmailData = account.getTwoFactorCodeForEmail();
        if (Strings.isNullOrEmpty(encryptedEmailData)) {
            throw new TwoFactorCredentialException(
                TwoFactorAuthConstants.ERROR_EMAIL_CODE_NOT_FOUND,
                account.getName(),
                acctNamePassedIn,
                "email code",
                CredentialErrorType.MISSING
            );
        }

        // Decrypt the email code data
        String decryptedEmailData = DataSource.decryptData(account.getId(), encryptedEmailData);

        // Parse the decrypted data
        return parseDecryptedData(decryptedEmailData, account.getName(), acctNamePassedIn);
    }

    /**
     * Parses decrypted email code data.
     * Splits the data into components and validates the format.
     *
     * @param decryptedData the decrypted email code data string
     * @param accountName the canonical account name
     * @param accountNamePassedIn the account name as passed in by the user
     * @return parsed email code data
     * @throws ServiceException if the data format is invalid
     */
    private static EmailCodeData parseDecryptedData(String decryptedData,
                                                    String accountName,
                                                    String accountNamePassedIn)
            throws ServiceException {
        // Split the data into parts
        String[] parts = decryptedData.split(TwoFactorAuthConstants.EMAIL_DATA_SEPARATOR);

        // Validate the format
        if (parts.length != TwoFactorAuthConstants.EMAIL_DATA_PARTS_COUNT) {
            throw new TwoFactorCredentialException(
                TwoFactorAuthConstants.ERROR_INVALID_EMAIL_CODE_FORMAT,
                accountName,
                accountNamePassedIn,
                "email code",
                CredentialErrorType.INVALID_FORMAT
            );
        }

        // Extract and parse the components
        try {
            String code = parts[TwoFactorAuthConstants.EMAIL_CODE_INDEX];
            long timestamp = Long.parseLong(parts[TwoFactorAuthConstants.EMAIL_TIMESTAMP_INDEX]);
            return new EmailCodeData(code, timestamp);
        } catch (NumberFormatException e) {
            throw new TwoFactorCredentialException(
                TwoFactorAuthConstants.ERROR_INVALID_EMAIL_TIMESTAMP,
                accountName,
                accountNamePassedIn,
                "email code timestamp",
                CredentialErrorType.CORRUPTED,
                e
            );
        }
    }

    /**
     * Validates an email code against stored data.
     * Checks if the provided code matches the stored code and hasn't expired.
     *
     * @param account the account to validate against
     * @param acctNamePassedIn the account name as passed in by the user
     * @param providedCode the code provided by the user
     * @param lifetimeMs the maximum lifetime of the code in milliseconds
     * @return the parsed email code data if validation succeeds
     * @throws ServiceException if validation fails or the code has expired
     */
    public static EmailCodeData validateAndParse(Account account, String acctNamePassedIn,
                                                 String providedCode, long lifetimeMs)
            throws ServiceException {
        EmailCodeData data = parse(account, acctNamePassedIn);

        // Check if expired
        if (data.isExpired(lifetimeMs)) {
            throw new com.btactic.twofactorauth.exception.TwoFactorCodeExpiredException(
                "The email 2FA code has expired",
                account.getName(),
                acctNamePassedIn,
                "Email",
                data.getExpiryTime(lifetimeMs)
            );
        }

        // Check if code matches
        if (!data.getCode().equals(providedCode)) {
            throw new com.btactic.twofactorauth.exception.TwoFactorCodeInvalidException(
                account.getName(),
                acctNamePassedIn,
                "Email",
                "code does not match expected value"
            );
        }

        return data;
    }
}
