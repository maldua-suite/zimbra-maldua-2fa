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
package com.btactic.twofactorauth.service;

/** SOAP handler to enable two-factor auth.
 * @author iraykin
 *
 */
public class ResetCodeMethod extends TwoFactorAuthMethod {

    @Override
    private SendTwoFactorAuthCodeResponse.SendTwoFactorAuthCodeStatus doMethod(Element request, Map<String, Object> context)
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

        // TODO: Add logic for when Reset cannot be done properly.
        if (true) {
          return SendTwoFactorAuthCodeStatus.RESET_SUCCEEDED;
        } else {
          return SendTwoFactorAuthCodeStatus.RESET_FAILED;
        }
    }

}
