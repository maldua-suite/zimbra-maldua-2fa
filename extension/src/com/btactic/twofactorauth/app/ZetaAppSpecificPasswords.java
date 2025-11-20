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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.account.auth.twofactor.AppSpecificPasswords;
import com.zimbra.cs.account.auth.twofactor.AppSpecificPasswordData;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.btactic.twofactorauth.core.BaseTwoFactorAuthComponent;
import com.btactic.twofactorauth.core.TwoFactorAuthUtils;
import com.btactic.twofactorauth.exception.TwoFactorCodeInvalidException;
import com.zimbra.cs.account.AppSpecificPassword;

/**
 * Manages application-specific passwords for two-factor authentication.
 * App-specific passwords allow applications that don't support 2FA
 * to authenticate with the account.
 *
 * @author iraykin
 *
 */
public class ZetaAppSpecificPasswords extends BaseTwoFactorAuthComponent implements AppSpecificPasswords {
    private Map<String, ZetaAppSpecificPassword> appPasswords = new HashMap<String, ZetaAppSpecificPassword>();

    public ZetaAppSpecificPasswords(Account account) throws ServiceException {
        this(account, account.getName());
    }

    public ZetaAppSpecificPasswords(Account account, String acctNamePassedIn) throws ServiceException {
        super(account, acctNamePassedIn);
        TwoFactorAuthUtils.disableTwoFactorAuthIfNecessary(account);
        if (account.isFeatureTwoFactorAuthAvailable()) {
            appPasswords = loadAppPasswords();
        }
    }

    public void clearData() throws ServiceException {
        revokeAll();
    }

    @Override
    public boolean isEnabled() throws ServiceException {
        if (twoFactorAuthRequired()) {
            return account.isFeatureAppSpecificPasswordsEnabled();
        } else {
            return false;
        }
    }

    @Override
    public void authenticate(String providedPassword) throws ServiceException {
        for (AppSpecificPassword appPassword: appPasswords.values())    {
            if (appPassword.validate(providedPassword)) {
                ZimbraLog.account.debug("logged in with app-specific password for account: " + account.getName());
                appPassword.update();
                return;
            }
        }
        ZimbraLog.account.error("invalid app-specific password for account: " + account.getName());
        throw new TwoFactorCodeInvalidException(
            account.getName(),
            acctNamePassedIn,
            "App-Specific Password",
            "password does not match any registered app-specific password"
        );
    }

    @Override
    public String getAppNameByPassword(String password) throws ServiceException {
        for (ZetaAppSpecificPassword appPassword: appPasswords.values())    {
            if (appPassword.validate(password)) {
                ZimbraLog.account.debug("getAppNameByPassword with app-specific password for account: " + account.getName());
                appPassword.update();
                return (appPassword.getName());
            }
        }
        ZimbraLog.account.error("invalid app-specific password in getAppNameByPassword for account: " + account.getName());
        throw new TwoFactorCodeInvalidException(
            account.getName(),
            acctNamePassedIn,
            "App-Specific Password",
            "password does not match any registered app-specific password"
        );
    }

    @Override
    public AppSpecificPassword generatePassword(String name) throws ServiceException {
        if (!account.isFeatureAppSpecificPasswordsEnabled()) {
            throw ServiceException.FAILURE("app-specific passwords are not enabled", new Throwable());
        }
        if (appPasswords.containsKey(name)) {
            throw ServiceException.FAILURE("app-specific password already exists for the name " + name, new Throwable());
        } else if (appPasswords.size() >= account.getMaxAppSpecificPasswords()) {
            throw ServiceException.FAILURE("app-specific password limit reached", new Throwable());
        }
        ZetaAppSpecificPassword password = ZetaAppSpecificPassword.generateNew(account, name);
        password.store();
        appPasswords.put(name, password);
        return password;
    }

    @Override
    public Set<AppSpecificPasswordData> getPasswords() throws ServiceException {
        Set<AppSpecificPasswordData> dataSet = new HashSet<AppSpecificPasswordData>();
        for (ZetaAppSpecificPassword appPassword: appPasswords.values()) {
            dataSet.add(appPassword.getPasswordData());
        }
        return dataSet;
    }

    @Override
    public void revoke(String name) throws ServiceException  {
        if (appPasswords.containsKey(name)) {
            appPasswords.get(name).revoke();
        } else {
            //if a password is not provisioned for this app, log but don't return an error
            ZimbraLog.account.error("no app-specific password provisioned for the name " + name);
        }
    }

    public int getNumAppPasswords() {
        return appPasswords.size();
    }

    private Map<String, ZetaAppSpecificPassword> loadAppPasswords() throws ServiceException {
        Map<String, ZetaAppSpecificPassword> passMap = new HashMap<String, ZetaAppSpecificPassword>();
        String[] passwords = account.getAppSpecificPassword();
        for (int i = 0; i < passwords.length; i++) {
            ZetaAppSpecificPassword entry = new ZetaAppSpecificPassword(account, passwords[i]);
            if (entry != null) {
                if (entry.isExpired()) {
                    entry.revoke();
                } else {
                    passMap.put(entry.getName(), entry);
                }
            }
        }
        return passMap;
    }

    @Override
    public void revokeAll() throws ServiceException {
        for (String name: appPasswords.keySet()) {
            revoke(name);
        }
    }

}
