/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra OSE 2FA Extension
 * Copyright (C) 2025 BTACTIC, S.C.C.L.
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
import com.zimbra.common.soap.Element;

// Only needed while debugging
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.service.account.AccountDocumentHandler;
import com.zimbra.soap.account.message.SendTwoFactorAuthCodeRequest;
import com.zimbra.soap.account.message.SendTwoFactorAuthCodeRequest.SendTwoFactorAuthCodeAction;
import com.zimbra.soap.account.message.SendTwoFactorAuthCodeResponse;
import com.zimbra.soap.account.message.SendTwoFactorAuthCodeResponse.SendTwoFactorAuthCodeStatus;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;

public class SendTwoFactorAuthCode extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraLog.webclient.info("MALDUA-2FA-DEBUG: Inside SendTwoFactorAuthCode function. 1");
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        ZimbraLog.webclient.info("MALDUA-2FA-DEBUG: Inside SendTwoFactorAuthCode function. 2");
        // Account account = getRequestedAccount(zsc);

        SendTwoFactorAuthCodeRequest req = JaxbUtil.elementToJaxb(request);
        ZimbraLog.webclient.info("MALDUA-2FA-DEBUG: Inside SendTwoFactorAuthCode function. 3");
        SendTwoFactorAuthCodeAction action = req.getAction();

        ZimbraLog.webclient.info("MALDUA-2FA-DEBUG: Inside SendTwoFactorAuthCode function. 4");

        SendTwoFactorAuthCodeResponse response = new SendTwoFactorAuthCodeResponse();
        ZimbraLog.webclient.info("MALDUA-2FA-DEBUG: Inside SendTwoFactorAuthCode function. 5");
        if (SendTwoFactorAuthCodeAction.EMAIL.equals(action)) {
            ZimbraLog.webclient.error("Maldua 2FA does not support email yet.");
            response.setStatus(SendTwoFactorAuthCodeStatus.NOT_SENT);
        } else if (SendTwoFactorAuthCodeAction.RESET.equals(action)) {
            ZimbraLog.webclient.info("MALDUA-2FA-DEBUG: Received action 'reset'");
            response.setStatus(SendTwoFactorAuthCodeStatus.RESET_SUCCEEDED);
            // TODO: Do something useful with this reset action
        } else {
           // Should not reach this point
           // TODO: Throw an SendTwoFactorAuthCodeException (to be created) exception
        }

        return zsc.jaxbToElement(response);
    }

}
