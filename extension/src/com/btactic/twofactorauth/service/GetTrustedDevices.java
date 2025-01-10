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

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.btactic.twofactorauth.trusteddevices.ZetaTrustedDevice;
import com.btactic.twofactorauth.trusteddevices.ZetaTrustedDeviceToken;
import com.btactic.twofactorauth.ZetaTwoFactorAuth;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.GetTrustedDevicesResponse;
import com.zimbra.cs.service.account.AccountDocumentHandler;

public class GetTrustedDevices extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        GetTrustedDevicesResponse response = new GetTrustedDevicesResponse();
        ZetaTwoFactorAuth manager = new ZetaTwoFactorAuth(account);
        if (!manager.twoFactorAuthEnabled()) {
            throw AccountServiceException.TWO_FACTOR_AUTH_REQUIRED();
        }
        List<ZetaTrustedDevice> devices = manager.getTrustedDevices();
        ZetaTrustedDeviceToken token = ZetaTrustedDeviceToken.fromRequest(account, request, context);
        boolean thisDeviceTrusted = false;
        int numOtherTrustedDevices = 0;
        if (token == null) {
            numOtherTrustedDevices = devices.size();
        } else {
            for (ZetaTrustedDevice td: devices) {
                if (token.getId().equals(td.getTokenId())) {
                    thisDeviceTrusted = true;
                } else {
                    numOtherTrustedDevices++;
                }
            }
        }
        response.setThisDeviceTrusted(thisDeviceTrusted);
        response.setNumOtherTrustedDevices(numOtherTrustedDevices);
        return zsc.jaxbToElement(response);
    }

}
