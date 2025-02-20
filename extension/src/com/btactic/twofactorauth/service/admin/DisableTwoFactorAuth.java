/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra OSE 2FA Extension
 * Copyright (C) 2025 BTACTIC, S.C.C.L.
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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.DisableTwoFactorAuthRequest;
import com.zimbra.soap.admin.message.DisableTwoFactorAuthResponse;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.cs.service.admin.AdminDocumentHandler;

import com.btactic.twofactorauth.ZetaTwoFactorAuth;

public class DisableTwoFactorAuth extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        DisableTwoFactorAuthRequest req = JaxbUtil.elementToJaxb(request);
        DisableTwoFactorAuthResponse resp = new DisableTwoFactorAuthResponse();

        AccountSelector acctSelector = req.getAccount();
        if (acctSelector == null) {
            throw ServiceException.INVALID_REQUEST("Must specify an account", null);
        }

        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(acctSelector);

        if (account == null) {
            throw AccountServiceException.NO_SUCH_ACCOUNT(acctSelector.getKey());
        }

        String method = req.getMethod();

        if (method.equals(AccountConstants.E_TWO_FACTOR_METHOD_APP)) {
            return handleTwoFactorDisable(zsc, account);
        } else if (method.equals(AccountConstants.E_TWO_FACTOR_METHOD_EMAIL)) {
            return handleEmailDisable(zsc, account);
        }

        throw ServiceException.INVALID_REQUEST("Unsupported 2FA method", null);

    }

    private Element handleEmailDisable(ZimbraSoapContext zsc, Account account)
            throws ServiceException {

        ZetaTwoFactorAuth manager = new ZetaTwoFactorAuth(account);
        DisableTwoFactorAuthResponse response = new DisableTwoFactorAuthResponse();
        manager.disableTwoFactorAuthEmail();
        return zsc.jaxbToElement(response);

    }

    private Element handleTwoFactorDisable(ZimbraSoapContext zsc, Account account)
            throws ServiceException {

        ZetaTwoFactorAuth manager = new ZetaTwoFactorAuth(account);
        DisableTwoFactorAuthResponse response = new DisableTwoFactorAuthResponse();
        manager.disableTwoFactorAuthApp(true);
        return zsc.jaxbToElement(response);

    }

}
