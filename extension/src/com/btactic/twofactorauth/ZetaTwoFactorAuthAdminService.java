/*
 * Maldua Zimbra 2FA Extension
 * Copyright (C) 2023 BTACTIC, S.C.C.L.
 */
package com.btactic.twofactorauth;

import com.zimbra.soap.DocumentDispatcher;
import com.zimbra.soap.DocumentService;
import com.zimbra.cs.service.admin.AdminService;

import com.zimbra.common.soap.AdminConstants;

import com.btactic.twofactorauth.service.admin.ClearTwoFactorAuthData;
import com.btactic.twofactorauth.service.admin.DisableTwoFactorAuth;
import com.btactic.twofactorauth.service.admin.GetClearTwoFactorAuthDataStatus;
import com.btactic.twofactorauth.service.admin.SendTwoFactorAuthCode;

public class ZetaTwoFactorAuthAdminService extends AdminService implements DocumentService {

    public void registerHandlers(DocumentDispatcher dispatcher) {
        dispatcher.registerHandler(AdminConstants.CLEAR_TWO_FACTOR_AUTH_DATA_REQUEST, new ClearTwoFactorAuthData());
        dispatcher.registerHandler(AdminConstants.DISABLE_TWO_FACTOR_AUTH_REQUEST, new DisableTwoFactorAuth());
        dispatcher.registerHandler(AdminConstants.GET_CLEAR_TWO_FACTOR_AUTH_DATA_STATUS_REQUEST, new GetClearTwoFactorAuthDataStatus());
        dispatcher.registerHandler(AdminConstants.SEND_TWO_FACTOR_AUTH_CODE_REQUEST, new SendTwoFactorAuthCode());
    }

}
