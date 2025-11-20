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
 * Centralizes magic strings, numbers, and default values.
 *
 * @author BTACTIC
 */
public final class TwoFactorAuthConstants {

    // Separators for data serialization
    public static final String EMAIL_DATA_SEPARATOR = ":";
    public static final String SECRET_SEPARATOR = "\\|";
    public static final String SCRATCH_CODE_SEPARATOR = ",";

    // Expected array lengths after split
    public static final int SECRET_PARTS_COUNT = 2;
    public static final int EMAIL_DATA_PARTS_COUNT = 3;
    public static final int SECRET_PARTS_COUNT_LEGACY = 1;

    // Indices for email data parts
    public static final int EMAIL_CODE_INDEX = 0;
    public static final int EMAIL_RESERVED_INDEX = 1;
    public static final int EMAIL_TIMESTAMP_INDEX = 2;

    // Indices for secret parts
    public static final int SECRET_VALUE_INDEX = 0;
    public static final int SECRET_TIMESTAMP_INDEX = 1;

    // Default encoding values
    public static final Encoding DEFAULT_SECRET_ENCODING = Encoding.BASE32;
    public static final Encoding DEFAULT_SCRATCH_ENCODING = Encoding.BASE32;

    // Error messages
    public static final String ERROR_INVALID_SECRET_FORMAT = "invalid shared secret format";
    public static final String ERROR_INVALID_SECRET_TIMESTAMP = "invalid shared secret timestamp";
    public static final String ERROR_INVALID_EMAIL_CODE_FORMAT = "invalid email code format";
    public static final String ERROR_INVALID_EMAIL_TIMESTAMP = "invalid email code timestamp format";
    public static final String ERROR_EMAIL_CODE_NOT_FOUND = "Email based 2FA code not found on server.";

    private TwoFactorAuthConstants() {
        // Prevent instantiation of utility class
        throw new AssertionError("Cannot instantiate constants class");
    }
}
