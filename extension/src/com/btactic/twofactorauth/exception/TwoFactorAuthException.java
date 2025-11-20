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

import com.zimbra.common.service.ServiceException;

/**
 * Base exception class for all two-factor authentication related errors.
 * This provides a more specific exception hierarchy than the generic ServiceException,
 * making error handling more precise and debugging easier.
 *
 * All custom 2FA exceptions should extend this class to maintain a consistent
 * exception hierarchy throughout the codebase.
 *
 * @author iraykin
 */
public class TwoFactorAuthException extends ServiceException {
    private static final long serialVersionUID = 1L;

    /**
     * The account name that encountered the 2FA error.
     */
    private final String accountName;

    /**
     * The account name as passed in by the user (may differ from canonical name).
     */
    private final String accountNamePassedIn;

    /**
     * Creates a new TwoFactorAuthException.
     *
     * @param message the error message
     * @param accountName the canonical account name
     * @param accountNamePassedIn the account name as provided by the user
     * @param cause the underlying cause
     */
    public TwoFactorAuthException(String message, String accountName,
                                  String accountNamePassedIn, Throwable cause) {
        super(message, FAILURE, RECEIVERS_FAULT, cause);
        this.accountName = accountName;
        this.accountNamePassedIn = accountNamePassedIn;
    }

    /**
     * Creates a new TwoFactorAuthException without a cause.
     *
     * @param message the error message
     * @param accountName the canonical account name
     * @param accountNamePassedIn the account name as provided by the user
     */
    public TwoFactorAuthException(String message, String accountName,
                                  String accountNamePassedIn) {
        this(message, accountName, accountNamePassedIn, null);
    }

    /**
     * Creates a new TwoFactorAuthException with only a message.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public TwoFactorAuthException(String message, Throwable cause) {
        this(message, null, null, cause);
    }

    /**
     * Creates a new TwoFactorAuthException with only a message.
     *
     * @param message the error message
     */
    public TwoFactorAuthException(String message) {
        this(message, null, null, null);
    }

    /**
     * Gets the canonical account name.
     *
     * @return the account name, or null if not set
     */
    public String getAccountName() {
        return accountName;
    }

    /**
     * Gets the account name as passed in by the user.
     *
     * @return the account name passed in, or null if not set
     */
    public String getAccountNamePassedIn() {
        return accountNamePassedIn;
    }

    /**
     * Gets a detailed error message including account information if available.
     *
     * @return a detailed error message
     */
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (accountName != null) {
            sb.append(" [account: ").append(accountName);
            if (accountNamePassedIn != null && !accountNamePassedIn.equals(accountName)) {
                sb.append(", provided as: ").append(accountNamePassedIn);
            }
            sb.append("]");
        }
        return sb.toString();
    }
}
