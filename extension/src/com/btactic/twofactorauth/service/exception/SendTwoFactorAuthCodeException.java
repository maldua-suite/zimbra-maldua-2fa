/*
 * ***** BEGIN LICENSE BLOCK *****
 * Maldua Zimbra 2FA Extension
 * (C) 2025 BTACTIC, S.C.C.L.
 *
 * Zimbra Collaboration Suite Server Copyright
 * (C) 2018 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>. *****
 * END LICENSE BLOCK *****
 */

// Adapted from com/zimbra/cs/account/ForgetPasswordException

package com.btactic.twofactorauth.service.exception;

import com.zimbra.cs.account.AccountServiceException;

@SuppressWarnings("serial")
public class SendTwoFactorAuthCodeException extends AccountServiceException {
    private static enum Codes{
        CODE_EXPIRED("account.CODE_EXPIRED"),
        TWO_FACTOR_AUTH_FAILED("account.TWO_FACTOR_AUTH_FAILED");

        private String code;
        private Codes(String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return code;
        }
    }

    protected SendTwoFactorAuthCodeException(String message, String code, boolean isReceiversFault) {
        this(message, code, isReceiversFault, null);
    }

    protected SendTwoFactorAuthCodeException(String message, String code, boolean isReceiversFault, Throwable cause) {
        super(message, code, isReceiversFault, cause);
    }

    public static AccountServiceException CODE_EXPIRED(String message) {
        return new SendTwoFactorAuthCodeException("service exception: " + message, Codes.CODE_EXPIRED.toString(), SENDERS_FAULT);
    }

    public static AccountServiceException TWO_FACTOR_AUTH_FAILED(String message) {
        return new SendTwoFactorAuthCodeException("service exception: " + message, Codes.TWO_FACTOR_AUTH_FAILED.toString(), SENDERS_FAULT);
    }

}
