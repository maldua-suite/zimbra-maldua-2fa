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
 * Exception thrown when a two-factor authentication code has expired.
 * This can occur for TOTP codes (typically valid for 30 seconds),
 * email-based codes (configurable timeout), or other time-sensitive codes.
 *
 * @author iraykin
 */
public class TwoFactorCodeExpiredException extends TwoFactorAuthException {
    private static final long serialVersionUID = 1L;

    private final String codeType;
    private final long expirationTime;

    /**
     * Creates a new TwoFactorCodeExpiredException.
     *
     * @param message the error message
     * @param accountName the canonical account name
     * @param accountNamePassedIn the account name as provided by the user
     * @param codeType the type of code that expired (e.g., "TOTP", "Email", "Scratch")
     * @param expirationTime the expiration timestamp in milliseconds
     */
    public TwoFactorCodeExpiredException(String message, String accountName,
                                        String accountNamePassedIn, String codeType,
                                        long expirationTime) {
        super(message, accountName, accountNamePassedIn);
        this.codeType = codeType;
        this.expirationTime = expirationTime;
    }

    /**
     * Creates a new TwoFactorCodeExpiredException.
     *
     * @param accountName the canonical account name
     * @param accountNamePassedIn the account name as provided by the user
     * @param codeType the type of code that expired
     */
    public TwoFactorCodeExpiredException(String accountName, String accountNamePassedIn,
                                        String codeType) {
        this("Two-factor authentication code has expired", accountName,
             accountNamePassedIn, codeType, System.currentTimeMillis());
    }

    /**
     * Gets the type of code that expired.
     *
     * @return the code type (e.g., "TOTP", "Email", "Scratch")
     */
    public String getCodeType() {
        return codeType;
    }

    /**
     * Gets the expiration timestamp.
     *
     * @return the expiration time in milliseconds since epoch
     */
    public long getExpirationTime() {
        return expirationTime;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (codeType != null) {
            sb.append(" [code type: ").append(codeType).append("]");
        }
        return sb.toString();
    }
}
