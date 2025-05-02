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
package com.btactic.twofactorauth.credentials;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.zimbra.cs.ldap.LdapDateUtil;

import com.zimbra.cs.account.auth.twofactor.TwoFactorAuth.Credentials;

public class TOTPCredentials implements Credentials {
    private String secret;
    private List<String> scratchCodes = new ArrayList<String>();
    private String timestamp;

    public TOTPCredentials(String secret, List<String> scratchCodes) {
        this.secret = secret;
        if (scratchCodes != null && !scratchCodes.isEmpty()) {
            this.scratchCodes = scratchCodes;
        }
        this.timestamp = LdapDateUtil.toGeneralizedTime(new Date());
    }

    public TOTPCredentials(String secret) {
        this(secret, null);
    }

    @Override
    public String getSecret() {
        return secret;
    }

    @Override
    public List<String> getScratchCodes() {
        return scratchCodes;
    }

    @Override
    public String getTimestamp() {
        return timestamp;
    }
}
