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
 * Exception thrown when two-factor authentication setup or configuration fails.
 * This can occur during:
 * - Initial 2FA enrollment
 * - Credential generation
 * - Method enablement (app-based, email-based)
 * - Configuration validation
 *
 * @author iraykin
 */
public class TwoFactorSetupException extends TwoFactorAuthException {
    private static final long serialVersionUID = 1L;

    private final String setupPhase;

    /**
     * Creates a new TwoFactorSetupException.
     *
     * @param message the error message
     * @param accountName the canonical account name
     * @param accountNamePassedIn the account name as provided by the user
     * @param setupPhase the phase of setup that failed (e.g., "credential generation", "enablement")
     * @param cause the underlying cause
     */
    public TwoFactorSetupException(String message, String accountName,
                                   String accountNamePassedIn, String setupPhase,
                                   Throwable cause) {
        super(message, accountName, accountNamePassedIn, cause);
        this.setupPhase = setupPhase;
    }

    /**
     * Creates a new TwoFactorSetupException without a cause.
     *
     * @param message the error message
     * @param accountName the canonical account name
     * @param accountNamePassedIn the account name as provided by the user
     * @param setupPhase the phase of setup that failed
     */
    public TwoFactorSetupException(String message, String accountName,
                                   String accountNamePassedIn, String setupPhase) {
        this(message, accountName, accountNamePassedIn, setupPhase, null);
    }

    /**
     * Creates a new TwoFactorSetupException.
     *
     * @param message the error message
     * @param setupPhase the phase of setup that failed
     * @param cause the underlying cause
     */
    public TwoFactorSetupException(String message, String setupPhase, Throwable cause) {
        this(message, null, null, setupPhase, cause);
    }

    /**
     * Creates a new TwoFactorSetupException.
     *
     * @param message the error message
     * @param setupPhase the phase of setup that failed
     */
    public TwoFactorSetupException(String message, String setupPhase) {
        this(message, null, null, setupPhase, null);
    }

    /**
     * Gets the phase of setup that failed.
     *
     * @return the setup phase (e.g., "credential generation", "enablement")
     */
    public String getSetupPhase() {
        return setupPhase;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (setupPhase != null) {
            sb.append(" [setup phase: ").append(setupPhase).append("]");
        }
        return sb.toString();
    }
}
