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
import com.btactic.twofactorauth.ZetaTwoFactorAuth;
import com.btactic.twofactorauth.app.ZetaAppSpecificPasswords;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.RevokeAppSpecificPasswordResponse;
import com.zimbra.cs.service.account.AccountDocumentHandler;

public class RevokeAppSpecificPassword extends AccountDocumentHandler {

	@Override
	public Element handle(Element request, Map<String, Object> context)
			throws ServiceException {
		ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        RevokeAppSpecificPasswordResponse response = new RevokeAppSpecificPasswordResponse();
        ZetaAppSpecificPasswords appSpecificPasswordsManager = new ZetaAppSpecificPasswords(account);
        String appName = request.getAttribute(AccountConstants.A_APP_NAME);
        appSpecificPasswordsManager.revoke(appName);
        return zsc.jaxbToElement(response);
	}
}
