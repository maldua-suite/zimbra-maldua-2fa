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
package com.btactic.twofactorauth.app;

import com.zimbra.cs.account.auth.twofactor.AppSpecificPasswordData;

public class ZetaAppSpecificPasswordData implements AppSpecificPasswordData {
    private String name;
    private String passwordHash;
    private Long dateCreated;
    private Long dateLastUsed;

    ZetaAppSpecificPasswordData(String name, String pass, Long dateCreated, Long dateLastUsed) {
        this.name = name;
        this.passwordHash = pass;
        this.dateCreated = dateCreated;
        this.dateLastUsed = dateLastUsed;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Long getDateCreated() {
        return dateCreated;
    }

    @Override
    public Long getDateLastUsed() {
        return dateLastUsed;
    }
}
