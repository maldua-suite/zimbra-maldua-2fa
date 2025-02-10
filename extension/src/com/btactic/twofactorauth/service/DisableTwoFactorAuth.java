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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.btactic.twofactorauth.ZetaTwoFactorAuth;
import com.btactic.twofactorauth.app.ZetaAppSpecificPasswords;
import com.zimbra.soap.account.message.DisableTwoFactorAuthResponse;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.cs.service.account.AccountDocumentHandler;

public class DisableTwoFactorAuth extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        Element methodEl = request.getOptionalElement(AccountConstants.E_METHOD);
        String method = null;
        if (methodEl != null) {
            method = methodEl.getText();
        }

        if (method.equals(AccountConstants.E_TWO_FACTOR_METHOD_APP)) {
            return handleTwoFactorDisable(request, context);
        } else if (method.equals(AccountConstants.E_TWO_FACTOR_METHOD_EMAIL)) {
            return handleEmailDisable(request, context);
        }

        throw AuthFailedServiceException.AUTH_FAILED("Unsupported 2FA method");

    }

    private Element handleEmailDisable(Element request, Map<String, Object> context)
            throws ServiceException {

        ZimbraSoapContext zsc = AccountDocumentHandler.getZimbraSoapContext(context);
        Account account = AccountDocumentHandler.getRequestedAccount(zsc);
        ZetaTwoFactorAuth manager = new ZetaTwoFactorAuth(account);
        DisableTwoFactorAuthResponse response = new DisableTwoFactorAuthResponse();
        manager.disableTwoFactorAuthEmail();
        return zsc.jaxbToElement(response);

    }

    private Element handleTwoFactorDisable(Element request, Map<String, Object> context)
            throws ServiceException {

        ZimbraSoapContext zsc = AccountDocumentHandler.getZimbraSoapContext(context);
        Account account = AccountDocumentHandler.getRequestedAccount(zsc);
        ZetaTwoFactorAuth manager = new ZetaTwoFactorAuth(account);
        DisableTwoFactorAuthResponse response = new DisableTwoFactorAuthResponse();
        manager.disableTwoFactorAuthApp(true);
        return zsc.jaxbToElement(response);

    }

}
