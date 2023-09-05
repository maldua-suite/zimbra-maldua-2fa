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
package com.btactic.twofactorauth.soap;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.btactic.twofactorauth.ZetaScratchCodes;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.GenerateScratchCodesResponse;
import com.zimbra.cs.service.account.AccountDocumentHandler;

public class GenerateScratchCodes extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        ZetaScratchCodes scratchCodesManager = new ZetaScratchCodes(account);
        if (!scratchCodesManager.twoFactorAuthEnabled()) {
            throw ServiceException.FAILURE("two-factor authentication is not enabled", new Throwable());
        }
        List<String> scratchCodes = scratchCodesManager.generateNewScratchCodes();
        GenerateScratchCodesResponse response = new GenerateScratchCodesResponse();
        response.setScratchCodes(scratchCodes);
        return zsc.jaxbToElement(response);
    }
}
