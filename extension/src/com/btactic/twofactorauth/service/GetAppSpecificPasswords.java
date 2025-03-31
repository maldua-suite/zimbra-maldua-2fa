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
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.btactic.twofactorauth.app.ZetaAppSpecificPasswords;
import com.btactic.twofactorauth.app.ZetaAppSpecificPasswordData;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.AppSpecificPasswordData;
import com.zimbra.soap.account.message.GetAppSpecificPasswordsResponse;
import com.zimbra.cs.service.account.AccountDocumentHandler;

public class GetAppSpecificPasswords extends AccountDocumentHandler {

	@Override
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

		ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        GetAppSpecificPasswordsResponse response = new GetAppSpecificPasswordsResponse();
        ZetaAppSpecificPasswords appManager = new ZetaAppSpecificPasswords(account);
        Set<com.zimbra.cs.account.auth.twofactor.AppSpecificPasswordData> names = appManager.getPasswords();
        encodeResponse(account, response, names);
        return zsc.jaxbToElement(response);
	}

	private void encodeResponse(Account acccount, GetAppSpecificPasswordsResponse response, Set<com.zimbra.cs.account.auth.twofactor.AppSpecificPasswordData> appPasswords) {
		for (com.zimbra.cs.account.auth.twofactor.AppSpecificPasswordData passwordData: appPasswords) {
		    AppSpecificPasswordData password = new AppSpecificPasswordData();
		    password.setAppName(passwordData.getName());
		    password.setDateCreated(passwordData.getDateCreated());
		    password.setDateLastUsed(passwordData.getDateLastUsed());
		    response.addAppSpecificPassword(password);
		}
		response.setMaxAppPasswords(acccount.getMaxAppSpecificPasswords());
	}
}
