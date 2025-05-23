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
package com.btactic.twofactorauth.service;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.account.AccountDocumentHandler;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.servlet.util.AuthUtil;
import com.zimbra.soap.account.message.SendTwoFactorAuthCodeRequest;
import com.zimbra.soap.account.message.SendTwoFactorAuthCodeRequest.SendTwoFactorAuthCodeAction;
import com.zimbra.soap.account.message.SendTwoFactorAuthCodeResponse;
import com.zimbra.soap.account.message.SendTwoFactorAuthCodeResponse.SendTwoFactorAuthCodeStatus;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;

public abstract class TwoFactorAuthMethod {

    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        ZimbraSoapContext zsc = AccountDocumentHandler.getZimbraSoapContext(context);
        SendTwoFactorAuthCodeRequest req = JaxbUtil.elementToJaxb(request);

        AuthToken at = AuthUtil.getAuthToken(request, zsc);
        Account authTokenAcct = AuthProvider.validateAuthToken(prov, at, false, Usage.TWO_FACTOR_AUTH);

        SendTwoFactorAuthCodeResponse response = new SendTwoFactorAuthCodeResponse();
        SendTwoFactorAuthCodeStatus sendTwoFactorAuthCodeStatus = doMethod(request,context);

        response.setStatus(sendTwoFactorAuthCodeStatus);

        return zsc.jaxbToElement(response);
    }

    protected abstract SendTwoFactorAuthCodeStatus doMethod(Element request, Map<String, Object> context)
            throws ServiceException;

    protected abstract SendTwoFactorAuthCodeAction getAction();

}
