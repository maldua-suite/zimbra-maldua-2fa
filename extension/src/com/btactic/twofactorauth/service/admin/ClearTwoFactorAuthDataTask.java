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
package com.btactic.twofactorauth.service.admin;

import static com.zimbra.common.util.TaskUtil.newDaemonThreadFactory;
import static java.util.concurrent.Executors.newCachedThreadPool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.btactic.twofactorauth.ZetaTwoFactorAuth;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchDirectoryOptions;
import com.zimbra.cs.account.SearchDirectoryOptions.ObjectType;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;

public class ClearTwoFactorAuthDataTask {

    private static final ExecutorService executor = newCachedThreadPool(newDaemonThreadFactory("ClearTwoFactorAuthData"));
    private Map<String, TaskStatus> tasksByCos = new HashMap<String, TaskStatus>();
    private static ClearTwoFactorAuthDataTask instance;

    public static enum TaskStatus {
        not_started, started, running, finished;
    }

    public static ClearTwoFactorAuthDataTask getInstance() {
        if (instance == null) {
            instance = new ClearTwoFactorAuthDataTask();
        }
        return instance;
    }

    public void clearAccount(Account account) throws ServiceException {
        ZetaTwoFactorAuth manager = new ZetaTwoFactorAuth(account);
        manager.clearData();
    }

    public TaskStatus clearCosAsync(final Cos cos) {
        final String cosId = cos.getId();
        synchronized (tasksByCos) {
            TaskStatus status = tasksByCos.get(cosId);
            if (status == TaskStatus.running) {
                ZimbraLog.account.debug("clear data task already running for cos " + cosId);
                return status;
            }
        }
        executor.submit(new Runnable() {
            @Override
            public void run() {
                tasksByCos.put(cosId, TaskStatus.running);
                Provisioning prov = Provisioning.getInstance();
                try {
                    Map<String, String> defaultCosIds = new HashMap<String, String>();
                    List<Domain> domains = prov.getAllDomains();
                    for (Domain d: domains) {
                        String defaultCosId = d.getDomainDefaultCOSId();
                        if (defaultCosId != null) {
                            defaultCosIds.put(d.getId(), defaultCosId);
                        }
                    }
                    SearchDirectoryOptions options = new SearchDirectoryOptions();
                    ZLdapFilter filter = ZLdapFilterFactory.getInstance().allAccountsOnlyByCos(cosId);
                    options.setFilter(filter);
                    options.setTypes(ObjectType.accounts);
                    List<NamedEntry> accounts = prov.searchDirectory(options);
                    for (NamedEntry entry: accounts) {
                        Account acct = (Account) entry;
                        String acctDomainId = acct.getDomainId();
                        String acctCosId = acct.getCOSId();
                        if (acctCosId == null) {
                            // This account has no COS specified, so check if the
                            // default COS for this account's domain is the one being acted on.
                            // If no default COS is specified on the account's domain, check if
                            // the requested COS is the global default.
                            String domainDefaultCos = defaultCosIds.get(acctDomainId);
                            if ((domainDefaultCos == null && !cos.isDefaultCos()) ||
                                    (domainDefaultCos != null && !domainDefaultCos.equals(cosId))) {
                                continue;
                            }
                        }
                        ZetaTwoFactorAuth manager;
                        try {
                            manager = new ZetaTwoFactorAuth(acct);
                            manager.clearData();
                        } catch (ServiceException e1) {
                            ZimbraLog.account.error("cannot clear two-factor auth data for account " + acct.getId());
                        }
                    }
                } catch (ServiceException e) {
                    ZimbraLog.account.error("error clearing two-factor auth data for account " + cos.getId());
                } finally {
                    tasksByCos.put(cosId, TaskStatus.finished);
                }
            }
        });
        return TaskStatus.started;
    }

    public TaskStatus getCosTaskStatus(String cosId) {
        TaskStatus status = tasksByCos.get(cosId);
        if (status == null) {
            return TaskStatus.not_started;
        } else {
            return status;
        }
    }
}
