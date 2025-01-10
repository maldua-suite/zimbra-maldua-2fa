/*
 * Zimbra OSE 2FA Extension
 * Copyright (C) 2023 BTACTIC, S.C.C.L.
 */
package com.btactic.twofactorauth;

import com.zimbra.soap.DocumentDispatcher;
import com.zimbra.soap.DocumentService;
import com.zimbra.cs.service.admin.AdminService;

import com.zimbra.common.soap.AdminConstants;

import com.btactic.twofactorauth.service.ClearTwoFactorAuthData;
import com.btactic.twofactorauth.service.GetClearTwoFactorAuthDataStatus;

public class ZetaTwoFactorAuthAdminService extends AdminService implements DocumentService {

    public void registerHandlers(DocumentDispatcher dispatcher) {
        dispatcher.registerHandler(AdminConstants.CLEAR_TWO_FACTOR_AUTH_DATA_REQUEST, new ClearTwoFactorAuthData());
        dispatcher.registerHandler(AdminConstants.GET_CLEAR_TWO_FACTOR_AUTH_DATA_STATUS_REQUEST, new GetClearTwoFactorAuthDataStatus());
    }

}
