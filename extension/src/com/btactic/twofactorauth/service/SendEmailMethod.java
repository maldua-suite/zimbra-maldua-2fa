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

import com.btactic.twofactorauth.ZetaTwoFactorAuth;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.cs.account.auth.AuthContext.Protocol;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.account.AccountDocumentHandler;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.mail.SetRecoveryAccount;
import com.zimbra.soap.account.message.EnableTwoFactorAuthResponse;
import com.zimbra.soap.account.message.DisableTwoFactorAuthResponse;
import com.zimbra.soap.account.message.SendTwoFactorAuthCodeRequest;
import com.zimbra.soap.account.message.SendTwoFactorAuthCodeResponse;
import com.zimbra.soap.account.message.SendTwoFactorAuthCodeResponse.SendTwoFactorAuthCodeStatus;
import com.zimbra.soap.mail.message.SetRecoveryAccountRequest;
import com.zimbra.soap.type.Channel;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

/** SOAP handler to enable two-factor auth.
 * @author iraykin
 *
 */
public class SendEmailMethod {

    public Element handleEnable(Element request, Map<String, Object> context)
            throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        ZimbraSoapContext zsc = AccountDocumentHandler.getZimbraSoapContext(context);
        String acctNamePassedIn = request.getElement(AccountConstants.E_NAME).getText();
        Account account = prov.get(AccountBy.name, acctNamePassedIn);
        if (account == null) {
            throw AuthFailedServiceException.AUTH_FAILED("no such account");
        }
        if (!account.isFeatureTwoFactorAuthAvailable()) {
            throw ServiceException.CANNOT_ENABLE_TWO_FACTOR_AUTH();
        }
        ZetaTwoFactorAuth manager = new ZetaTwoFactorAuth(account, acctNamePassedIn);

        if (!manager.isAllowedMethod(AccountConstants.E_TWO_FACTOR_METHOD_EMAIL)) {
            throw ServiceException.CANNOT_ENABLE_TWO_FACTOR_AUTH();
        }

        Element passwordEl = request.getOptionalElement(AccountConstants.E_PASSWORD);
        String password = null;
        if (passwordEl != null) {
            password = passwordEl.getText();
        }

        Element emailEl = request.getOptionalElement(AccountConstants.E_EMAIL);
        String email = null;
        if (emailEl != null) {
            email = emailEl.getText();
        }

        Element twoFactorCodeEl = request.getOptionalElement(AccountConstants.E_TWO_FACTOR_CODE);
        String twoFactorCode = null;
        if (twoFactorCodeEl != null) {
            twoFactorCode = twoFactorCodeEl.getText();
        }

        if ( (password != null) && (email != null) ) {
          account.authAccount(password, Protocol.soap);
          resetCode(context);
          sendCode(email,context);
        } else if (twoFactorCode != null) {
          validateCode(twoFactorCode, context);
          manager.enableTwoFactorAuth();
          manager.addEnabledMethod(AccountConstants.E_TWO_FACTOR_METHOD_EMAIL);
        } else {
          throw ServiceException.FAILURE("Non supported wizard input.", null);
        }

        EnableTwoFactorAuthResponse response = new EnableTwoFactorAuthResponse();
        HttpServletRequest httpReq = (HttpServletRequest)context.get(SoapServlet.SERVLET_REQUEST);
        HttpServletResponse httpResp = (HttpServletResponse)context.get(SoapServlet.SERVLET_RESPONSE);
        try {
            AuthToken at = AuthProvider.getAuthToken(account);
            response.setAuthToken(new com.zimbra.soap.account.type.AuthToken(at.getEncoded(), false));
            at.encode(httpResp, false, ZimbraCookie.secureCookie(httpReq), false);
        } catch (AuthTokenException e) {
            throw ServiceException.FAILURE("cannot generate auth token", e);
        }

        return zsc.jaxbToElement(response);
    }

    private void resetCode(Map<String, Object> context) throws ServiceException {
        SetRecoveryAccountRequest resetRecoveryAccountRequest = new SetRecoveryAccountRequest();
        resetRecoveryAccountRequest.setOp(SetRecoveryAccountRequest.Op.reset);
        resetRecoveryAccountRequest.setChannel(Channel.EMAIL);
        Element resetReq = JaxbUtil.jaxbToElement(resetRecoveryAccountRequest);
        resetReq.addAttribute("isFromEnableTwoFactorAuth", true);

        try {
            // TODO: Check if reusing context here is a good idea or if we should create a new one
            new SetRecoveryAccount().handle(resetReq, context);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("Cannot reset the code", e);
        }
    }

    private void sendCode(String email, Map<String, Object> context) throws ServiceException {
        SetRecoveryAccountRequest setRecoveryAccountRequest = new SetRecoveryAccountRequest();
        setRecoveryAccountRequest.setOp(SetRecoveryAccountRequest.Op.sendCode);
        setRecoveryAccountRequest.setRecoveryAccount(email);
        setRecoveryAccountRequest.setChannel(Channel.EMAIL);
        Element setReq = JaxbUtil.jaxbToElement(setRecoveryAccountRequest);
        setReq.addAttribute("isFromEnableTwoFactorAuth", true);

        try {
            // TODO: Check if reusing context here is a good idea or if we should create a new one
            new SetRecoveryAccount().handle(setReq, context);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("Cannot send the code by email", e);
        }
    }

    private void validateCode(String twoFactorCode, Map<String, Object> context) throws ServiceException {
        SetRecoveryAccountRequest setRecoveryAccountRequest = new SetRecoveryAccountRequest();
        setRecoveryAccountRequest.setOp(SetRecoveryAccountRequest.Op.validateCode);
        setRecoveryAccountRequest.setRecoveryAccountVerificationCode(twoFactorCode);
        setRecoveryAccountRequest.setChannel(Channel.EMAIL);
        Element setReq = JaxbUtil.jaxbToElement(setRecoveryAccountRequest);
        setReq.addAttribute("isFromEnableTwoFactorAuth", true);

        try {
            // TODO: Check if reusing context here is a good idea or if we should create a new one
            new SetRecoveryAccount().handle(setReq, context);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("Cannot validate the code", e);
        }
    }

    public Element handleDisable(Element request, Map<String, Object> context)
            throws ServiceException {

        ZimbraSoapContext zsc = AccountDocumentHandler.getZimbraSoapContext(context);
        Account account = AccountDocumentHandler.getRequestedAccount(zsc);
        ZetaTwoFactorAuth manager = new ZetaTwoFactorAuth(account);
        DisableTwoFactorAuthResponse response = new DisableTwoFactorAuthResponse();
        manager.disableTwoFactorAuthEmail();
        return zsc.jaxbToElement(response);

    }

    public Element handleSendTwoFactorAuthCode(Element request, Map<String, Object> context)
            throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        ZimbraSoapContext zsc = AccountDocumentHandler.getZimbraSoapContext(context);
        SendTwoFactorAuthCodeRequest req = JaxbUtil.elementToJaxb(request);

        AuthToken at;
        Account authTokenAcct;

        at = zsc.getAuthToken();
        authTokenAcct = AuthProvider.validateAuthToken(prov, at, false, Usage.TWO_FACTOR_AUTH);

        SendTwoFactorAuthCodeResponse response = new SendTwoFactorAuthCodeResponse();

        String recoveryEmail = authTokenAcct.getPrefPasswordRecoveryAddress();

        if (recoveryEmail != null) {
          resetCode(context);
          try {
            sendCode(recoveryEmail,context);
          } catch (ServiceException e) {
            response.setStatus(SendTwoFactorAuthCodeStatus.NOT_SENT);
          }
        } else {
          throw ServiceException.FAILURE("Non supported wizard input.", null);
        }

        response.setStatus(SendTwoFactorAuthCodeStatus.SENT);

        return zsc.jaxbToElement(response);
    }

}
