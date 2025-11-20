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
import com.btactic.twofactorauth.exception.TwoFactorAuthRequiredException;
import com.btactic.twofactorauth.exception.TwoFactorSetupException;
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
            return handleTwoFactorEnable(request, context);
        } else if (method.equals(AccountConstants.E_TWO_FACTOR_METHOD_EMAIL)) {
            return handleEmailEnable(request, context);
        }

        throw new TwoFactorSetupException("Unsupported 2FA method: " + method, "method validation");
    }

    private Element handleEmailEnable(Element request, Map<String, Object> context)
            throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        ZimbraSoapContext zsc = AccountDocumentHandler.getZimbraSoapContext(context);
        String acctNamePassedIn = request.getElement(AccountConstants.E_NAME).getText();
        Account account = prov.get(AccountBy.name, acctNamePassedIn);
        if (account == null) {
            throw AuthFailedServiceException.AUTH_FAILED("no such account");
        }
        if (!account.isFeatureTwoFactorAuthAvailable()) {
            throw new TwoFactorAuthRequiredException(
                acctNamePassedIn,
                acctNamePassedIn,
                AccountConstants.E_TWO_FACTOR_METHOD_EMAIL,
                false
            );
        }
        ZetaTwoFactorAuth manager = new ZetaTwoFactorAuth(account, acctNamePassedIn);

        if (!manager.isAllowedMethod(AccountConstants.E_TWO_FACTOR_METHOD_EMAIL)) {
            throw new TwoFactorSetupException(
                "Email-based 2FA method is not allowed for this account",
                acctNamePassedIn,
                acctNamePassedIn,
                "method authorization check"
            );
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
          if (!(account.isTwoFactorAuthEnabled())) {
              manager.generateCredentials();
              manager.enableTwoFactorAuth();
          }
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

    /**
     * Resets the recovery code for the account.
     * Delegates to SetRecoveryAccount handler with reset operation.
     *
     * @param context the SOAP context containing authentication and session info
     * @throws ServiceException if reset operation fails
     */
    private void resetCode(Map<String, Object> context) throws ServiceException {
        SetRecoveryAccountRequest resetRecoveryAccountRequest = new SetRecoveryAccountRequest();
        resetRecoveryAccountRequest.setOp(SetRecoveryAccountRequest.Op.reset);
        resetRecoveryAccountRequest.setChannel(Channel.EMAIL);
        Element resetReq = JaxbUtil.jaxbToElement(resetRecoveryAccountRequest);
        resetReq.addAttribute("isFromEnableTwoFactorAuth", true);

        try {
            // Context reuse is safe here - it preserves authentication state and session info
            // which is necessary for the delegated handler to access the authenticated account
            new SetRecoveryAccount().handle(resetReq, context);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("Cannot reset the code", e);
        }
    }

    /**
     * Sends a verification code to the specified email address.
     * Delegates to SetRecoveryAccount handler with sendCode operation.
     *
     * @param email the email address to send the code to
     * @param context the SOAP context containing authentication and session info
     * @throws ServiceException if send operation fails
     */
    private void sendCode(String email, Map<String, Object> context) throws ServiceException {
        SetRecoveryAccountRequest setRecoveryAccountRequest = new SetRecoveryAccountRequest();
        setRecoveryAccountRequest.setOp(SetRecoveryAccountRequest.Op.sendCode);
        setRecoveryAccountRequest.setRecoveryAccount(email);
        setRecoveryAccountRequest.setChannel(Channel.EMAIL);
        Element setReq = JaxbUtil.jaxbToElement(setRecoveryAccountRequest);
        setReq.addAttribute("isFromEnableTwoFactorAuth", true);

        try {
            // Context reuse is safe here - it preserves authentication state and session info
            // which is necessary for the delegated handler to access the authenticated account
            new SetRecoveryAccount().handle(setReq, context);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("Cannot send the code by email", e);
        }
    }

    /**
     * Validates a verification code provided by the user.
     * Delegates to SetRecoveryAccount handler with validateCode operation.
     *
     * @param twoFactorCode the verification code to validate
     * @param context the SOAP context containing authentication and session info
     * @throws ServiceException if validation fails or code is invalid
     */
    private void validateCode(String twoFactorCode, Map<String, Object> context) throws ServiceException {
        SetRecoveryAccountRequest setRecoveryAccountRequest = new SetRecoveryAccountRequest();
        setRecoveryAccountRequest.setOp(SetRecoveryAccountRequest.Op.validateCode);
        setRecoveryAccountRequest.setRecoveryAccountVerificationCode(twoFactorCode);
        setRecoveryAccountRequest.setChannel(Channel.EMAIL);
        Element setReq = JaxbUtil.jaxbToElement(setRecoveryAccountRequest);
        setReq.addAttribute("isFromEnableTwoFactorAuth", true);

        try {
            // Context reuse is safe here - it preserves authentication state and session info
            // which is necessary for the delegated handler to access the authenticated account
            new SetRecoveryAccount().handle(setReq, context);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("Cannot validate the code", e);
        }
    }

    /**
     * Validates account and checks if 2FA is available and allowed.
     *
     * @param request the SOAP request element
     * @return array containing [Account, ZetaTwoFactorAuth, String accountName]
     * @throws ServiceException if account doesn't exist or 2FA is not available
     */
    private Object[] validateAndGetAccount(Element request) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        String acctNamePassedIn = request.getElement(AccountConstants.E_NAME).getText();
        Account account = prov.get(AccountBy.name, acctNamePassedIn);

        if (account == null) {
            throw AuthFailedServiceException.AUTH_FAILED("no such account");
        }
        if (!account.isFeatureTwoFactorAuthAvailable()) {
            throw new TwoFactorAuthRequiredException(
                acctNamePassedIn,
                acctNamePassedIn,
                AccountConstants.E_TWO_FACTOR_METHOD_APP,
                false
            );
        }

        ZetaTwoFactorAuth manager = new ZetaTwoFactorAuth(account, acctNamePassedIn);
        if (!manager.isAllowedMethod(AccountConstants.E_TWO_FACTOR_METHOD_APP)) {
            throw new TwoFactorSetupException(
                "App-based 2FA method is not allowed for this account",
                acctNamePassedIn,
                acctNamePassedIn,
                "method authorization check"
            );
        }

        return new Object[]{account, manager, acctNamePassedIn};
    }

    /**
     * Handles the initial setup phase (generates credentials and returns secret).
     *
     * @param account the user account
     * @param manager the 2FA manager
     * @param password the user's password for authentication
     * @param response the response object to populate
     * @throws ServiceException if setup fails
     */
    private void handleInitialSetup(Account account, ZetaTwoFactorAuth manager, String password,
                                   EnableTwoFactorAuthResponse response) throws ServiceException {
        account.authAccount(password, Protocol.soap);
        if (manager.isEnabledMethod(AccountConstants.E_TWO_FACTOR_METHOD_APP)) {
            encodeAlreadyEnabled(response);
        } else {
            if (!account.isTwoFactorAuthEnabled()) {
                manager.generateCredentials();
            }
            response.setSecret(manager.loadSharedSecret());
            try {
                String token = AuthProvider.getAuthToken(account, Usage.ENABLE_TWO_FACTOR_AUTH).getEncoded();
                com.zimbra.soap.account.type.AuthToken at = new com.zimbra.soap.account.type.AuthToken(token, false);
                response.setAuthToken(at);
            } catch (AuthTokenException e) {
                throw ServiceException.FAILURE("cannot generate auth token", e);
            }
        }
    }

    /**
     * Authenticates the request using either auth token or password.
     *
     * @param request the SOAP request element
     * @param account the user account
     * @param password the user's password (may be null)
     * @throws ServiceException if authentication fails
     */
    private void authenticateRequest(Element request, Account account, String password) throws ServiceException {
        Element authTokenEl = request.getOptionalElement(AccountConstants.E_AUTH_TOKEN);
        if (authTokenEl != null) {
            authenticateWithAuthToken(authTokenEl, account);
        } else if (password != null) {
            account.authAccount(password, Protocol.soap);
        } else {
            throw AuthFailedServiceException.AUTH_FAILED("auth token and password missing");
        }
    }

    /**
     * Authenticates using an auth token from the request.
     *
     * @param authTokenEl the auth token element
     * @param account the user account
     * @throws ServiceException if token is invalid
     */
    private void authenticateWithAuthToken(Element authTokenEl, Account account) throws ServiceException {
        AuthToken at = null;
        try {
            at = AuthProvider.getAuthToken(authTokenEl, account);
            Provisioning prov = Provisioning.getInstance();
            Account authTokenAcct = AuthProvider.validateAuthToken(prov, at, false, Usage.ENABLE_TWO_FACTOR_AUTH);
            boolean verifyAccount = authTokenEl.getAttributeBool(AccountConstants.A_VERIFY_ACCOUNT, false);
            if (verifyAccount && !authTokenAcct.getId().equalsIgnoreCase(account.getId())) {
                throw AuthFailedServiceException.AUTH_FAILED("auth token doesn't match the named account");
            }
        } catch (AuthTokenException e) {
            throw AuthFailedServiceException.AUTH_FAILED("invalid auth token");
        } finally {
            if (at != null) {
                try {
                    at.deRegister();
                } catch (AuthTokenException e) {
                    ZimbraLog.account.warn("could not de-register two-factor authentication auth token");
                }
            }
        }
    }

    /**
     * Generates and encodes the final auth token for the response.
     *
     * @param account the user account
     * @param context the SOAP context
     * @param response the response object to populate
     * @throws ServiceException if token generation fails
     */
    private void generateFinalAuthToken(Account account, Map<String, Object> context,
                                      EnableTwoFactorAuthResponse response) throws ServiceException {
        HttpServletRequest httpReq = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
        HttpServletResponse httpResp = (HttpServletResponse) context.get(SoapServlet.SERVLET_RESPONSE);
        try {
            AuthToken at = AuthProvider.getAuthToken(account);
            response.setAuthToken(new com.zimbra.soap.account.type.AuthToken(at.getEncoded(), false));
            at.encode(httpResp, false, ZimbraCookie.secureCookie(httpReq), false);
        } catch (AuthTokenException e) {
            throw ServiceException.FAILURE("cannot generate auth token", e);
        }
    }

    private Element handleTwoFactorEnable(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = AccountDocumentHandler.getZimbraSoapContext(context);

        // Validate account and get necessary objects
        Object[] validation = validateAndGetAccount(request);
        Account account = (Account) validation[0];
        ZetaTwoFactorAuth manager = (ZetaTwoFactorAuth) validation[1];
        String acctNamePassedIn = (String) validation[2];

        EnableTwoFactorAuthResponse response = new EnableTwoFactorAuthResponse();

        // Extract password from request
        Element passwordEl = request.getOptionalElement(AccountConstants.E_PASSWORD);
        String password = (passwordEl != null) ? passwordEl.getText() : null;

        // Check if this is initial setup or verification phase
        Element twoFactorCode = request.getOptionalElement(AccountConstants.E_TWO_FACTOR_CODE);

        if (twoFactorCode == null) {
            // Initial setup phase: authenticate and generate credentials
            handleInitialSetup(account, manager, password, response);
        } else {
            // Verification phase: verify TOTP code and activate 2FA
            authenticateRequest(request, account, password);
            manager.authenticateTOTP(twoFactorCode.getText());

            if (!account.isTwoFactorAuthEnabled()) {
                manager.enableTwoFactorAuth();
            }
            manager.addEnabledMethod(AccountConstants.E_TWO_FACTOR_METHOD_APP);

            // Return scratch codes
            ZetaScratchCodes scratchCodesManager = new ZetaScratchCodes(account);
            response.setScratchCodes(scratchCodesManager.getCodes());

            // Update token validity
            int tokenValidityValue = account.getAuthTokenValidityValue();
            account.setAuthTokenValidityValue(
                tokenValidityValue == Integer.MAX_VALUE ? 0 : tokenValidityValue + 1
            );

            // Generate and encode final auth token
            generateFinalAuthToken(account, context, response);
        }

        return zsc.jaxbToElement(response);
    }

    private void encodeAlreadyEnabled(EnableTwoFactorAuthResponse response) {}

    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }
}
