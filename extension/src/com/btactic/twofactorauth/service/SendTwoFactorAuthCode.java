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
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;

import com.btactic.twofactorauth.service.exception.SendTwoFactorAuthCodeException;

public class SendTwoFactorAuthCode extends AccountDocumentHandler {

    List<Class<?>> methodClassList = new ArrayList<Class<?>>();

    methodClassList.add(ResetCodeMethod.class);
    methodClassList.add(SendEmailMethod.class);

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        SendTwoFactorAuthCodeRequest req = JaxbUtil.elementToJaxb(request);
        SendTwoFactorAuthCodeAction action = req.getAction();

        // After studying SendTwoFactorAuthCodeTag.java:
        // 'app' method is converted into 'reset' action. Then it should be handled by 'ResetCodeMethod.java'.
        // 'email' method is converted into 'email' action. Then it should be handled by 'SendEmailMethod.java'.
        // ResetCodeMethod and SendEmailMethod are classes which are children of TwoFactorAuthMethod class.

        // TODO: Convert onto an ArrayList of classes which can be instantiated
        // and thanks to its getMethod you can decide to use them or not

        TwoFactorAuthMethod method;
        for (int i = 0; i < methodClassList.size(); i++) {
            Class<?> methodClass = methodClassList.get(i);
            method = methodClass.getDeclaredConstructor().newInstance();
            if method.getAction().equals(action) {
              return method.handle(request, context);
            }
        }

        throw SendTwoFactorAuthCodeException.TWO_FACTOR_AUTH_FAILED("Unsupported 2FA method");
    }

    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }

}
