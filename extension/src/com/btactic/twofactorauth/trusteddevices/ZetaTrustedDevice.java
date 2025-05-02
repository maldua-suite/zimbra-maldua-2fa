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
package com.btactic.twofactorauth.trusteddevices;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.BEncoding;
import com.zimbra.common.util.BEncoding.BEncodingException;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.TrustedDevice;
import com.zimbra.cs.account.TrustedDeviceToken;

public class ZetaTrustedDevice implements TrustedDevice {

    private Account account;
    private Map <String, Object> deviceAttrs = new HashMap<String, Object>();
    private ZetaTrustedDeviceToken token;
    private Integer trustedTokenId;
    private Long expires;
    private DeviceVerification verification;

    public ZetaTrustedDevice(Account account, Map<String, Object> attrs) {
        this.account = account;
        this.deviceAttrs = attrs;
        this.token = new ZetaTrustedDeviceToken(account, this);
        this.trustedTokenId = token.getId();
        this.expires = token.getExpires();
        setVerificationMechanism();
    }

    public static ZetaTrustedDevice byTrustedToken(Account acct, TrustedDeviceToken token) throws ServiceException {
        for (String encodedDevice: acct.getTwoFactorAuthTrustedDevices()) {
            if (encodedDevice.startsWith(String.valueOf(token.getId()))) {
                return new ZetaTrustedDevice(acct, encodedDevice);
            }
        }
        return null;
    }

    public ZetaTrustedDevice(Account account, String encoded) throws ServiceException {
        this.account = account;
        String[] parts = encoded.split("\\|", 3);
        if (parts.length != 3) {
            throw ServiceException.FAILURE("cannot decoded trusted device info", new Throwable());
        }
        expires = Long.valueOf(parts[1]);
        trustedTokenId = Integer.parseInt(parts[0]);
        String encodedMap = parts[2];
        try {
            deviceAttrs = BEncoding.decode(encodedMap);
        } catch (BEncodingException e) {
            throw ServiceException.FAILURE("cannot decoded trusted device info", e);
        }
        setVerificationMechanism();
    }

    private void setVerificationMechanism() {
        if (deviceAttrs.get(AuthContext.AC_DEVICE_ID) != null) {
            this.verification = new DeviceIdVerification(this);
        } else {
            this.verification = new DummyVerification(this);
        }
    }

    private String encode() {
        return String.format("%d|%d|%s", trustedTokenId, expires, BEncoding.encode(deviceAttrs));
    }

    public void register() throws ServiceException {
        account.addTwoFactorAuthTrustedDevices(encode());
    }

    public void revoke() throws ServiceException {
        account.removeTwoFactorAuthTrustedDevices(encode());
    }

    public boolean verify(Map<String, Object> attrs) {
        return verification.verify(attrs);
    }

    public Map<String, Object> getAttrs() {
        return deviceAttrs;
    }

    public ZetaTrustedDeviceToken getToken() {
        return token;
    }

    public Integer getTokenId() {
        return trustedTokenId;
    }

    public long getExpires() {
        return expires;
    }

    public boolean isExpired() {
        return expires < System.currentTimeMillis();
    }

    public abstract class DeviceVerification {
        protected ZetaTrustedDevice trustedDevice;

        public DeviceVerification(ZetaTrustedDevice trustedDevice) {
            this.trustedDevice = trustedDevice;
        }

        public abstract boolean verify(Map<String, Object> attrs);
    }

    private abstract class AttributeDeviceVerification extends DeviceVerification {

        public AttributeDeviceVerification(ZetaTrustedDevice trustedDevice) {
            super(trustedDevice);
        }

        protected boolean verifyAttribute(Map<String, Object> attrsPassedIn, String attrName) {
            Map<String, Object> attrs = trustedDevice.getAttrs();
            if (attrs.get(attrName) == null || attrsPassedIn.get(attrName) == null) {
                return false;
            }
            String expected = (String) attrs.get(attrName);
            String actual = (String) attrsPassedIn.get(attrName);
            return expected.equalsIgnoreCase(actual);
        }
    }

    public class DeviceIdVerification extends AttributeDeviceVerification {

        public DeviceIdVerification(ZetaTrustedDevice trustedDevice) {
            super(trustedDevice);
        }

        @Override
        public boolean verify(Map<String, Object> attrs) {
            return verifyAttribute(attrs, AuthContext.AC_DEVICE_ID);
        }
    }

    public class DummyVerification extends DeviceIdVerification {

        public DummyVerification(ZetaTrustedDevice trustedDevice) {
            super(trustedDevice);
        }

        @Override
        public boolean verify(Map<String, Object> attrs) {
            return true;
        }
    }
}
