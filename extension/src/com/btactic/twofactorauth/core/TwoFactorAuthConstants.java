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

import com.zimbra.common.auth.twofactor.TwoFactorOptions.Encoding;

/**
 * Constants used throughout the 2FA extension.
 * Centralizes magic strings, numbers, and default values to improve maintainability
 * and prevent errors from scattered literal values.
 *
 * <p>This class cannot be instantiated and should only be used as a constant provider.
 *
 * @author BTACTIC
 */
public final class TwoFactorAuthConstants {

    /** Separator character for email-based 2FA data (code:reserved:timestamp format). */
    public static final String EMAIL_DATA_SEPARATOR = ":";

    /** Separator character for shared secret data (secret|timestamp format). Escaped for regex use. */
    public static final String SECRET_SEPARATOR = "\\|";

    /** Separator character for comma-delimited scratch codes list. */
    public static final String SCRATCH_CODE_SEPARATOR = ",";

    /** Expected number of parts when splitting a valid shared secret (secret + timestamp). */
    public static final int SECRET_PARTS_COUNT = 2;

    /** Expected number of parts when splitting email 2FA data (code + reserved + timestamp). */
    public static final int EMAIL_DATA_PARTS_COUNT = 3;

    /** Legacy format: secret without timestamp (single part only). */
    public static final int SECRET_PARTS_COUNT_LEGACY = 1;

    /** Index of the code value in split email data array. */
    public static final int EMAIL_CODE_INDEX = 0;

    /** Index of reserved field in split email data array (currently unused). */
    public static final int EMAIL_RESERVED_INDEX = 1;

    /** Index of the timestamp value in split email data array. */
    public static final int EMAIL_TIMESTAMP_INDEX = 2;

    /** Index of the secret value in split secret data array. */
    public static final int SECRET_VALUE_INDEX = 0;

    /** Index of the timestamp value in split secret data array. */
    public static final int SECRET_TIMESTAMP_INDEX = 1;

    /** Default encoding for TOTP shared secrets (BASE32 for Google Authenticator compatibility). */
    public static final Encoding DEFAULT_SECRET_ENCODING = Encoding.BASE32;

    /** Default encoding for scratch codes (BASE32 for consistency). */
    public static final Encoding DEFAULT_SCRATCH_ENCODING = Encoding.BASE32;

    /** Error message when shared secret has invalid format (missing parts or malformed). */
    public static final String ERROR_INVALID_SECRET_FORMAT = "invalid shared secret format";

    /** Error message when shared secret timestamp cannot be parsed. */
    public static final String ERROR_INVALID_SECRET_TIMESTAMP = "invalid shared secret timestamp";

    /** Error message when email code data has invalid format. */
    public static final String ERROR_INVALID_EMAIL_CODE_FORMAT = "invalid email code format";

    /** Error message when email code timestamp cannot be parsed as a number. */
    public static final String ERROR_INVALID_EMAIL_TIMESTAMP = "invalid email code timestamp format";

    /** Error message when no email-based 2FA code is found in the account data. */
    public static final String ERROR_EMAIL_CODE_NOT_FOUND = "Email based 2FA code not found on server.";

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class that should only provide constants.
     *
     * @throws AssertionError always, to prevent reflection-based instantiation
     */
    private TwoFactorAuthConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}
