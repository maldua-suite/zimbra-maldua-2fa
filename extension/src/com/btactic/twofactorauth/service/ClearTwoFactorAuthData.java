/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra OSE 2FA Extension
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
package com.btactic.twofactorauth.service;

import java.util.Date;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.btactic.twofactorauth.ClearTwoFactorAuthDataTask;
import com.btactic.twofactorauth.ClearTwoFactorAuthDataTask.TaskStatus;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.ClearTwoFactorAuthDataRequest;
import com.zimbra.soap.admin.message.ClearTwoFactorAuthDataResponse;
import com.zimbra.soap.admin.type.CosSelector;
import com.zimbra.soap.admin.type.CosSelector.CosBy;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.type.ZmBoolean;
import com.zimbra.cs.service.admin.AdminDocumentHandler;

public class ClearTwoFactorAuthData extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        ClearTwoFactorAuthDataRequest req = JaxbUtil.elementToJaxb(request);
        ClearTwoFactorAuthDataResponse resp = new ClearTwoFactorAuthDataResponse();
        AccountSelector acctSelector = req.getAccount();
        CosSelector cosSelector = req.getCos();
        Boolean lazy = req.getLazyDelete() != null ? ZmBoolean.toBool(req.getLazyDelete()) : true;
        if (acctSelector == null && cosSelector == null) {
            throw ServiceException.INVALID_REQUEST("must specify an account or COS", null);
        }
        if (acctSelector != null && cosSelector != null) {
            throw ServiceException.INVALID_REQUEST("cannot specify both account and COS", null);
        }
        if (acctSelector != null) {
            Account account = prov.get(acctSelector);
            if (account == null) {
                throw AccountServiceException.NO_SUCH_ACCOUNT(acctSelector.getKey());
            } else {
                ClearTwoFactorAuthDataTask clearDataTask = ClearTwoFactorAuthDataTask.getInstance();
                clearDataTask.clearAccount(account);
            }
        } else {
            Cos cos;
            if (cosSelector.getBy() == CosBy.id) {
                cos = prov.get(com.zimbra.common.account.Key.CosBy.id, cosSelector.getKey());
            } else {
                cos = prov.get(com.zimbra.common.account.Key.CosBy.name, cosSelector.getKey());
            }
            if (cos == null) {
                throw AccountServiceException.NO_SUCH_COS(cosSelector.getKey());
            } else {
                if (lazy) {
                    cos.setTwoFactorAuthLastReset(new Date());
                } else {
                    ClearTwoFactorAuthDataTask clearDataTask = ClearTwoFactorAuthDataTask.getInstance();
                    TaskStatus status = clearDataTask.clearCosAsync(cos);
                    resp.setStatus(status.toString());
                }
            }
        }
        return zsc.jaxbToElement(resp);
    }
}
