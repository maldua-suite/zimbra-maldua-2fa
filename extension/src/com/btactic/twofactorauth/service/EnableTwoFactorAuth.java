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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.AuthContext.Protocol;
import com.btactic.twofactorauth.credentials.TOTPCredentials;
import com.btactic.twofactorauth.ZetaTwoFactorAuth;
import com.btactic.twofactorauth.ZetaScratchCodes;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.mail.SetRecoveryAccount;
import com.zimbra.soap.account.message.EnableTwoFactorAuthResponse;
import com.zimbra.soap.mail.message.SetRecoveryAccountRequest;
import com.zimbra.soap.type.Channel;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.cs.service.account.AccountDocumentHandler;

/** SOAP handler to enable two-factor auth.
 * @author iraykin
 *
 */
public class EnableTwoFactorAuth extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        Element methodEl = request.getOptionalElement(AccountConstants.E_METHOD);
        String method = null;
        if (methodEl != null) {
            method = methodEl.getText();
        }

        if (method.equals(AccountConstants.E_TWO_FACTOR_METHOD_APP)) {
            TwoFactorAuthMethod twoFactorAuthMethod = new TwoFactorAuthMethod();
            return twoFactorAuthMethod.handleEnable(request, context);
        } else if (method.equals(AccountConstants.E_TWO_FACTOR_METHOD_EMAIL)) {
            SendEmailMethod sendEmailMethod = new SendEmailMethod();
            return sendEmailMethod.handleEnable(request, context);
        }

        throw AuthFailedServiceException.AUTH_FAILED("Unsupported 2FA method");
    }

    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }
}
