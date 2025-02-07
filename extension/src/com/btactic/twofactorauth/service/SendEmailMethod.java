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
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.account.AccountDocumentHandler;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.soap.account.message.SendTwoFactorAuthCodeRequest;
import com.zimbra.soap.account.message.SendTwoFactorAuthCodeRequest.SendTwoFactorAuthCodeAction;
import com.zimbra.soap.account.message.SendTwoFactorAuthCodeResponse;
import com.zimbra.soap.account.message.SendTwoFactorAuthCodeResponse.SendTwoFactorAuthCodeStatus;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;

import com.btactic.twofactorauth.ZetaTwoFactorAuth;

/** SOAP handler to enable two-factor auth.
 * @author iraykin
 *
 */
public class SendEmailMethod extends TwoFactorAuthMethod {

    @Override
    protected SendTwoFactorAuthCodeStatus doMethod(Element request, Map<String, Object> context)
            throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        ZimbraSoapContext zsc = AccountDocumentHandler.getZimbraSoapContext(context);
        SendTwoFactorAuthCodeRequest req = JaxbUtil.elementToJaxb(request);

        AuthToken at;
        Account authTokenAcct;

        // TODO: Should we get the AuthToken from the SendTwoFactorAuthCodeRequest
        // instead of zcs
        // because the token is sent at the same level of action in `SendTwoFactorAuthCodeTag.java` file
        // ?
        at = zsc.getAuthToken();
        authTokenAcct = AuthProvider.validateAuthToken(prov, at, false, Usage.TWO_FACTOR_AUTH);

        String recoveryEmail = authTokenAcct.getPrefPasswordRecoveryAddress();
        boolean emailIsSent = false;
        if (recoveryEmail != null) {
          try {
            String code = "1234444";
            ZetaTwoFactorAuth manager = new ZetaTwoFactorAuth(authTokenAcct);
            manager.storeEmailCode(code);
            // sendCode(recoveryEmail,code);
            emailIsSent = true;
          } catch (ServiceException e) {
            emailIsSent = false;
          }
        } else {
          throw ServiceException.FAILURE("Non supported wizard input.", null);
        }

        // TODO: Add logic for when sending email cannot be done properly.
        if (emailIsSent) {
          return SendTwoFactorAuthCodeStatus.SENT;
        } else {
          return SendTwoFactorAuthCodeStatus.NOT_SENT;
        }
    }

    protected SendTwoFactorAuthCodeAction getAction() {
        return SendTwoFactorAuthCodeAction.EMAIL;
    }

}
