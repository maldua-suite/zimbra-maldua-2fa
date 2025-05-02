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

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.BEncoding;
import com.zimbra.common.util.BEncoding.BEncodingException;
import com.zimbra.common.util.RandomPassword;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.auth.PasswordUtil;
import com.zimbra.cs.account.auth.twofactor.AppSpecificPasswordData;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AppSpecificPassword;

public class ZetaAppSpecificPassword implements AppSpecificPassword {
    private Account account;
    private String appPassword;
    private String hashedPassword;
    private String appName;
    private Long dateCreated;
    private Long prevDateLastUsed;
    private Long dateLastUsed;
    private static final String NAME_KEY = "n";
    private static final String PASS_KEY = "p";
    private static final String DATE_CREATED_KEY = "dc";
    private static final String DATE_LAST_USED_KEY = "du";

    public static ZetaAppSpecificPassword generateNew(Account account, String name) throws ServiceException {
        String randomPassword = generatePassword(account);
        Long curTime = System.currentTimeMillis();
        ZetaAppSpecificPassword password = new ZetaAppSpecificPassword(account, name, randomPassword, curTime);
        return password;
    }

    private static String generatePassword(Account account) throws ServiceException {
        int passwordLength = Provisioning.getInstance().getConfig().getAppSpecificPasswordLength();
        return RandomPassword.generate(passwordLength, passwordLength, RandomPassword.ALPHABET_ONLY_LETTERS);
    }

    @Override
    public void store() throws ServiceException {
        account.addAppSpecificPassword(toLdapEntry());
    }

    @Override
    public void update() throws ServiceException {
        account.removeAppSpecificPassword(toLdapEntry(true));
        store();
    }

    private String toLdapEntry() {
        return toLdapEntry(false);
    }

    private ZetaAppSpecificPassword(Account account, String name, String password, Long dateCreated) {
        this(account, name, password, dateCreated, null, false);
    }

    public ZetaAppSpecificPassword(Account account, String name, String password, Long dateCreated, Long dateLastUsed) {
        this(account, name, password, dateCreated, dateLastUsed, true);
    }

    public ZetaAppSpecificPassword(Account account, String name, String password, Long dateCreated, Long dateLastUsed, boolean alreadyHashed) {
        this.account = account;
        this.appName = name;
        if (alreadyHashed) {
            appPassword = null;
            hashedPassword = password;
        } else {
            appPassword = password;
            hashedPassword = hash(password);
        }
        setDateCreated(dateCreated);
        setDateLastUsed(dateLastUsed);
    }

    public ZetaAppSpecificPassword(Account account, String encoded) {
        this(account, ldapToData(encoded));
    }

    private ZetaAppSpecificPassword(Account account, ZetaAppSpecificPasswordData data) {
        this(account, data.getName(), data.getPassword(), data.getDateCreated(), data.getDateLastUsed());
    }

    private String hash(String password) {
        return PasswordUtil.SSHA512.generateSSHA512(password, null);
    }

    @Override
    public String getName() {
        return appName;
    }

    @Override
    public String getPassword() {
        return appPassword;
    }

    @Override
    public void setDateLastUsed(Long date) {
        prevDateLastUsed = dateLastUsed;
        dateLastUsed = date;
    }

    @Override
    public void setDateCreated(Long date) {
        this.dateCreated = date;
    }

    @Override
    public Long getDateLastUsed() {
        return dateLastUsed;
    }

    @Override
    public Long getDateCreated() {
        return dateCreated;
    }

    private String getPasswordHash() {
        return hashedPassword;
    }

    @Override
    public boolean validate(String providedPassword) throws ServiceException {
        if (PasswordUtil.SSHA512.verifySSHA512(getPasswordHash(), providedPassword)) {
            setDateLastUsed(System.currentTimeMillis());
            return true;
        } else {
            return false;
        }
    }

    private String toLdapEntry(boolean changed) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(NAME_KEY, appName);
        map.put(PASS_KEY, hashedPassword);
        map.put(DATE_CREATED_KEY, dateCreated);
        map.put(DATE_LAST_USED_KEY, changed ? prevDateLastUsed : dateLastUsed);
        return BEncoding.encode(map);
    }

    private static ZetaAppSpecificPasswordData ldapToData(String encoded) {
        String name;
        String pass;
        Long created;
        Long lastUsed;
        Map<String, Object> decoded;
        try {
            decoded = BEncoding.decode(encoded);
        } catch (BEncodingException e) {
            ZimbraLog.account.error("could not decode app-specific password");
            return null;
        }
        name = (String) decoded.get(NAME_KEY);
        pass = (String) decoded.get(PASS_KEY);
        created = (Long) decoded.get(DATE_CREATED_KEY);
        lastUsed = (Long) decoded.get(DATE_LAST_USED_KEY);
        return new ZetaAppSpecificPasswordData(name, pass, created, lastUsed);
    }

    @Override
    public void revoke() throws ServiceException {
        account.removeAppSpecificPassword(toLdapEntry());
    }

    @Override
    public AppSpecificPasswordData getPasswordData() {
        return new ZetaAppSpecificPasswordData(appName, null, dateCreated, dateLastUsed);
    }

    @Override
    public boolean isExpired() {
        Long dateCreated = getDateCreated();
        Long passwordLifetime = account.getAppSpecificPasswordDuration();
        if (passwordLifetime == 0L) {
            // app-specific passwords do not expire
            return false;
        }
        Long expiresAt = dateCreated + passwordLifetime;
        return expiresAt < System.currentTimeMillis();
    }

}
