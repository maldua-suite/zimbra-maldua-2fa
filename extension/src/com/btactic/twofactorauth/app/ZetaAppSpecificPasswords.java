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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.zimbra.common.auth.twofactor.AuthenticatorConfig;
import com.zimbra.common.auth.twofactor.TwoFactorOptions.CodeLength;
import com.zimbra.common.auth.twofactor.TwoFactorOptions.HashAlgorithm;
import com.zimbra.cs.account.auth.twofactor.AppSpecificPasswords;
import com.zimbra.cs.account.auth.twofactor.AppSpecificPasswordData;
import com.zimbra.cs.account.auth.twofactor.TwoFactorAuth.CredentialConfig;
import com.zimbra.common.auth.twofactor.TwoFactorOptions.Encoding;
import com.zimbra.common.auth.twofactor.TOTPAuthenticator;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.btactic.twofactorauth.ZetaTwoFactorAuth;
import com.btactic.twofactorauth.app.ZetaAppSpecificPassword;
import com.btactic.twofactorauth.app.ZetaAppSpecificPasswordData;
import com.zimbra.cs.account.AppSpecificPassword;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.TrustedDevice;
import com.btactic.twofactorauth.trusteddevices.ZetaTrustedDevice;
import com.btactic.twofactorauth.trusteddevices.ZetaTrustedDeviceToken;
import com.zimbra.cs.account.ldap.ChangePasswordListener;
import com.zimbra.cs.account.ldap.LdapLockoutPolicy;
import com.zimbra.cs.ldap.LdapDateUtil;

/**
 * This class is the main entry point for two-factor authentication.
 *
 * @author iraykin
 *
 */
public class ZetaAppSpecificPasswords implements AppSpecificPasswords {
    private Account account;
    private String acctNamePassedIn;
    private String secret;
    private List<String> scratchCodes;
    private Encoding encoding;
    private Encoding scratchEncoding;
    boolean hasStoredSecret;
    boolean hasStoredScratchCodes;
    private Map<String, ZetaAppSpecificPassword> appPasswords = new HashMap<String, ZetaAppSpecificPassword>();

    public ZetaAppSpecificPasswords(Account account) throws ServiceException {
        this(account, account.getName());
    }

    public ZetaAppSpecificPasswords(Account account, String acctNamePassedIn) throws ServiceException {
        this.account = account;
        this.acctNamePassedIn = acctNamePassedIn;
        ZetaTwoFactorAuth manager = new ZetaTwoFactorAuth(account, acctNamePassedIn);
        manager.disableTwoFactorAuthIfNecessary();
        if (account.isFeatureTwoFactorAuthAvailable()) {
            appPasswords = loadAppPasswords();
        }
    }

    public void clearData() throws ServiceException {
        revokeAll();
    }

    /* Determine if a second factor is necessary for authenticating this account */
    public boolean twoFactorAuthRequired() throws ServiceException {
        if (!account.isFeatureTwoFactorAuthAvailable()) {
            return false;
        } else {
            boolean isRequired = account.isFeatureTwoFactorAuthRequired();
            boolean isUserEnabled = account.isTwoFactorAuthEnabled();
            return isUserEnabled || isRequired;
        }
    }

    @Override
    public boolean isEnabled() throws ServiceException {
        if (twoFactorAuthRequired()) {
            return account.isFeatureAppSpecificPasswordsEnabled();
        } else {
            return false;
        }
    }

    private static String decrypt(Account account, String encrypted) throws ServiceException {
        return DataSource.decryptData(account.getId(), encrypted);
    }

    @Override
    public void authenticate(String providedPassword) throws ServiceException {
        for (AppSpecificPassword appPassword: appPasswords.values())    {
            if (appPassword.validate(providedPassword)) {
                ZimbraLog.account.debug("logged in with app-specific password");
                appPassword.update();
                return;
            }
        }
        throw AuthFailedServiceException.TWO_FACTOR_AUTH_FAILED(account.getName(), acctNamePassedIn, "invalid app-specific password");
    }

    @Override
    public String getAppNameByPassword(String password) throws ServiceException {
        for (ZetaAppSpecificPassword appPassword: appPasswords.values())    {
            if (appPassword.validate(password)) {
                ZimbraLog.account.debug("getAppNameByPassword with app-specific password");
                appPassword.update();
                return (appPassword.getName());
            }
        }
        throw AuthFailedServiceException.TWO_FACTOR_AUTH_FAILED(account.getName(), acctNamePassedIn, "invalid app-specific password");
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
