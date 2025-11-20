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
 * Exception thrown when two-factor authentication is required for an account
 * but is not properly configured or enabled.
 * This can occur when:
 * - Admin policy requires 2FA but user hasn't set it up
 * - 2FA feature is not available for the account
 * - Required 2FA method is not enabled
 *
 * @author iraykin
 */
public class TwoFactorAuthRequiredException extends TwoFactorAuthException {
    private static final long serialVersionUID = 1L;

    private final String requiredMethod;
    private final boolean isAdminRequired;

    /**
     * Creates a new TwoFactorAuthRequiredException.
     *
     * @param message the error message
     * @param accountName the canonical account name
     * @param accountNamePassedIn the account name as provided by the user
     * @param requiredMethod the specific 2FA method required (e.g., "app", "email")
     * @param isAdminRequired true if 2FA is required by admin policy
     */
    public TwoFactorAuthRequiredException(String message, String accountName,
                                         String accountNamePassedIn, String requiredMethod,
                                         boolean isAdminRequired) {
        super(message, accountName, accountNamePassedIn);
        this.requiredMethod = requiredMethod;
        this.isAdminRequired = isAdminRequired;
    }

    /**
     * Creates a new TwoFactorAuthRequiredException.
     *
     * @param accountName the canonical account name
     * @param accountNamePassedIn the account name as provided by the user
     * @param requiredMethod the specific 2FA method required
     * @param isAdminRequired true if 2FA is required by admin policy
     */
    public TwoFactorAuthRequiredException(String accountName, String accountNamePassedIn,
                                         String requiredMethod, boolean isAdminRequired) {
        this("Two-factor authentication is required but not properly configured",
             accountName, accountNamePassedIn, requiredMethod, isAdminRequired);
    }

    /**
     * Creates a new TwoFactorAuthRequiredException.
     *
     * @param accountName the canonical account name
     * @param accountNamePassedIn the account name as provided by the user
     */
    public TwoFactorAuthRequiredException(String accountName, String accountNamePassedIn) {
        this(accountName, accountNamePassedIn, null, false);
    }

    /**
     * Gets the specific 2FA method that is required.
     *
     * @return the required method (e.g., "app", "email"), or null if not specified
     */
    public String getRequiredMethod() {
        return requiredMethod;
    }

    /**
     * Checks if 2FA is required by administrator policy.
     *
     * @return true if required by admin policy, false if required by user preference
     */
    public boolean isAdminRequired() {
        return isAdminRequired;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        sb.append(" [");
        if (isAdminRequired) {
            sb.append("required by admin policy");
        } else {
            sb.append("required by user");
        }
        if (requiredMethod != null) {
            sb.append(", method: ").append(requiredMethod);
        }
        sb.append("]");
        return sb.toString();
    }
}
