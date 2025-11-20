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
 * Exception thrown when a two-factor authentication code is invalid.
 * This can occur when:
 * - The code format is incorrect
 * - The code doesn't match the expected value
 * - The code has already been used (for one-time codes like scratch codes)
 *
 * @author iraykin
 */
public class TwoFactorCodeInvalidException extends TwoFactorAuthException {
    private static final long serialVersionUID = 1L;

    private final String codeType;
    private final String reason;

    /**
     * Creates a new TwoFactorCodeInvalidException.
     *
     * @param message the error message
     * @param accountName the canonical account name
     * @param accountNamePassedIn the account name as provided by the user
     * @param codeType the type of code that was invalid (e.g., "TOTP", "Email", "Scratch")
     * @param reason the specific reason the code was invalid
     */
    public TwoFactorCodeInvalidException(String message, String accountName,
                                        String accountNamePassedIn, String codeType,
                                        String reason) {
        super(message, accountName, accountNamePassedIn);
        this.codeType = codeType;
        this.reason = reason;
    }

    /**
     * Creates a new TwoFactorCodeInvalidException.
     *
     * @param accountName the canonical account name
     * @param accountNamePassedIn the account name as provided by the user
     * @param codeType the type of code that was invalid
     * @param reason the specific reason the code was invalid
     */
    public TwoFactorCodeInvalidException(String accountName, String accountNamePassedIn,
                                        String codeType, String reason) {
        this("Invalid two-factor authentication code", accountName,
             accountNamePassedIn, codeType, reason);
    }

    /**
     * Creates a new TwoFactorCodeInvalidException without specific reason.
     *
     * @param accountName the canonical account name
     * @param accountNamePassedIn the account name as provided by the user
     * @param codeType the type of code that was invalid
     */
    public TwoFactorCodeInvalidException(String accountName, String accountNamePassedIn,
                                        String codeType) {
        this(accountName, accountNamePassedIn, codeType, null);
    }

    /**
     * Gets the type of code that was invalid.
     *
     * @return the code type (e.g., "TOTP", "Email", "Scratch")
     */
    public String getCodeType() {
        return codeType;
    }

    /**
     * Gets the reason the code was invalid.
     *
     * @return the reason, or null if not specified
     */
    public String getReason() {
        return reason;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (codeType != null) {
            sb.append(" [code type: ").append(codeType);
            if (reason != null) {
                sb.append(", reason: ").append(reason);
            }
            sb.append("]");
        }
        return sb.toString();
    }
}
