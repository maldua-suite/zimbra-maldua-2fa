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
package com.btactic.twofactorauth.exception;

/**
 * Exception thrown when there are issues with two-factor authentication credentials.
 * This can occur when:
 * - Credentials are missing or not found
 * - Credentials are corrupted or invalid format
 * - Decryption of stored credentials fails
 * - Credential storage/retrieval fails
 *
 * @author iraykin
 */
public class TwoFactorCredentialException extends TwoFactorAuthException {
    private static final long serialVersionUID = 1L;

    private final String credentialType;
    private final CredentialErrorType errorType;

    /**
     * Enum representing different types of credential errors.
     */
    public enum CredentialErrorType {
        MISSING("Credentials not found"),
        INVALID_FORMAT("Invalid credential format"),
        DECRYPTION_FAILED("Credential decryption failed"),
        STORAGE_FAILED("Credential storage failed"),
        CORRUPTED("Credentials corrupted");

        private final String description;

        CredentialErrorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Creates a new TwoFactorCredentialException.
     *
     * @param message the error message
     * @param accountName the canonical account name
     * @param accountNamePassedIn the account name as provided by the user
     * @param credentialType the type of credential (e.g., "secret", "scratch codes", "app password")
     * @param errorType the type of credential error
     * @param cause the underlying cause
     */
    public TwoFactorCredentialException(String message, String accountName,
                                       String accountNamePassedIn, String credentialType,
                                       CredentialErrorType errorType, Throwable cause) {
        super(message, accountName, accountNamePassedIn, cause);
        this.credentialType = credentialType;
        this.errorType = errorType;
    }

    /**
     * Creates a new TwoFactorCredentialException without a cause.
     *
     * @param message the error message
     * @param accountName the canonical account name
     * @param accountNamePassedIn the account name as provided by the user
     * @param credentialType the type of credential
     * @param errorType the type of credential error
     */
    public TwoFactorCredentialException(String message, String accountName,
                                       String accountNamePassedIn, String credentialType,
                                       CredentialErrorType errorType) {
        this(message, accountName, accountNamePassedIn, credentialType, errorType, null);
    }

    /**
     * Creates a new TwoFactorCredentialException.
     *
     * @param accountName the canonical account name
     * @param accountNamePassedIn the account name as provided by the user
     * @param credentialType the type of credential
     * @param errorType the type of credential error
     */
    public TwoFactorCredentialException(String accountName, String accountNamePassedIn,
                                       String credentialType, CredentialErrorType errorType) {
        this(errorType.getDescription(), accountName, accountNamePassedIn,
             credentialType, errorType);
    }

    /**
     * Creates a new TwoFactorCredentialException.
     *
     * @param message the error message
     * @param credentialType the type of credential
     * @param errorType the type of credential error
     * @param cause the underlying cause
     */
    public TwoFactorCredentialException(String message, String credentialType,
                                       CredentialErrorType errorType, Throwable cause) {
        this(message, null, null, credentialType, errorType, cause);
    }

    /**
     * Gets the type of credential that had an error.
     *
     * @return the credential type (e.g., "secret", "scratch codes")
     */
    public String getCredentialType() {
        return credentialType;
    }

    /**
     * Gets the type of error that occurred.
     *
     * @return the credential error type
     */
    public CredentialErrorType getErrorType() {
        return errorType;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (credentialType != null || errorType != null) {
            sb.append(" [");
            if (credentialType != null) {
                sb.append("credential type: ").append(credentialType);
                if (errorType != null) {
                    sb.append(", ");
                }
            }
            if (errorType != null) {
                sb.append("error: ").append(errorType.getDescription());
            }
            sb.append("]");
        }
        return sb.toString();
    }
}
