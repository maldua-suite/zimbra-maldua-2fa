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

import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Hex;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.BlobMetaData;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.SoapServlet;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.TrustedTokenKey;
import com.zimbra.cs.account.TokenUtil;
import com.zimbra.cs.account.TrustedDeviceToken;

public class ZetaTrustedDeviceToken implements TrustedDeviceToken {
    private Integer tokenId;
    private Long expires;
    private ZetaTrustedDevice device;
    private static final String TOKEN_ID = "tid";
    private boolean deleted = false;

    public ZetaTrustedDeviceToken(String encoded) throws AuthTokenException, ServiceException {
        decode(encoded);
    }

    public ZetaTrustedDeviceToken(Account account, ZetaTrustedDevice device) {
        this.tokenId = new Random().nextInt(Integer.MAX_VALUE-1) + 1;
        this.expires = account.getTwoFactorAuthTrustedDeviceTokenLifetime() + System.currentTimeMillis();
        this.device = device;
    }

    private void decode(String encoded) throws AuthTokenException, ServiceException {
        int pos = encoded.indexOf('_');
        if (pos == -1) {
            throw new AuthTokenException("invalid trusted device token format");
        }
        String ver = encoded.substring(0, pos);

        int pos2 = encoded.indexOf('_', pos+1);
        if (pos2 == -1) {
            throw new AuthTokenException("invalid trusted device token format");
        }
        String hmac = encoded.substring(pos+1, pos2);
        String data = encoded.substring(pos2+1);

        TrustedTokenKey key = TrustedTokenKey.getVersion(ver);
        if (key == null) {
            throw new AuthTokenException("unknown key version");
        }
        String computedHmac = TokenUtil.getHmac(data, key.getKey());
        if (!computedHmac.equals(hmac)) {
            throw new AuthTokenException("hmac failure");
        }
        Map<?, ?> map = TokenUtil.getAttrs(data);
        tokenId = Integer.parseInt((String) map.get(TOKEN_ID));
    }

    public void setExpires(long expires) {
        this.expires = expires;
    }

    private String getTokenString() throws ServiceException {
        StringBuilder sb = new StringBuilder();
        BlobMetaData.encodeMetaData(TOKEN_ID, tokenId, sb);
        String data = new String(Hex.encodeHex(sb.toString().getBytes()));
        TrustedTokenKey key = TrustedTokenKey.getCurrentKey();
        String hmac = TokenUtil.getHmac(data, key.getKey());
        return key.getVersion() + "_" + hmac + "_" + data;
    }

    public static ZetaTrustedDeviceToken fromRequest(Account account, Element request, Map<String, Object> context)
            throws ServiceException {
        if (account == null) {
            return null;
        }
        String encodedToken = null;
        try {
            encodedToken = request.getElement(AccountConstants.E_TRUSTED_TOKEN).getText();
        } catch (ServiceException e) {}
        if (encodedToken == null) {
            HttpServletRequest req = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
            String cookieName = ZimbraCookie.COOKIE_ZM_TRUST_TOKEN;
            javax.servlet.http.Cookie cookies[] =  req.getCookies();
            if (cookies == null) {
                return null;
            }
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(cookieName)) {
                    encodedToken = cookies[i].getValue();
                    break;
                }
            }
        }
        if (encodedToken != null && !encodedToken.isEmpty()) {
            try {
                ZetaTrustedDeviceToken token = new ZetaTrustedDeviceToken(encodedToken);
                // we want to catch tokens that don't have corresponding devices early
                ZetaTrustedDevice device = ZetaTrustedDevice.byTrustedToken(account, token);
                if (device == null) {
                    ZimbraLog.account.debug("cannot find trusted device for trusted device token");
                    token.setDelete();
                    return token;
                } else {
                    if (device.isExpired()) {
                        device.revoke();
                        token.setDelete();
                        return token;
                    } else {
                        token.setExpires(device.getExpires());
                        return token;
                    }
                }
            } catch (AuthTokenException e) {
                ZimbraLog.account.warn("invalid trusted device token format");
                return null;
            }
        } else {
            return null;
        }
    }

    public Integer getId() {
        return tokenId;
    }

    public Long getExpires() {
        return expires;
    }

    public void setDelete() {
        this.deleted = true;
    }

    private Integer getTokenExpiry() {
        long timeLeft = expires - System.currentTimeMillis();
        return Integer.valueOf((int)(timeLeft / 1000));
    }

    public void encode(HttpServletResponse resp, Element el, boolean secure) throws ServiceException {
        String name = ZimbraCookie.COOKIE_ZM_TRUST_TOKEN;
        String path = ZimbraCookie.PATH_ROOT;
        Integer expiresIn;
        if (deleted) {
            expiresIn = 0;
        } else {
            expiresIn = getTokenExpiry();
            if (expiresIn < 0) {
                expiresIn = 0;
            }
        }
        el.addUniqueElement(AccountConstants.E_TRUSTED_TOKEN).setText(getTokenString());
        if (expiresIn > 0) {
            ZimbraCookie.addHttpOnlyCookie(resp, name, getTokenString(), path, expiresIn, secure);
            el.addUniqueElement(AccountConstants.E_TRUST_LIFETIME).setText(String.valueOf(expires - System.currentTimeMillis()));
        } else {
            ZimbraCookie.clearCookie(resp, name);
            el.addUniqueElement(AccountConstants.E_TRUST_LIFETIME).setText("0");
        }
    }

    public boolean isExpired() {
        return deleted || expires < System.currentTimeMillis();
    }
}
