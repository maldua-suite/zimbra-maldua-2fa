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
package com.btactic.twofactorauth.credentials;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Base64;

import com.zimbra.cs.account.auth.twofactor.TwoFactorAuth.CredentialConfig;
import com.zimbra.common.auth.twofactor.TwoFactorOptions.Encoding;
import com.zimbra.common.service.ServiceException;

public class CredentialGenerator {
    private final CredentialConfig config;
    private final SecureRandom secureRandom;

    public CredentialGenerator(CredentialConfig config) {
        this.config = config;
        // Use default SecureRandom implementation (more secure than SHA1PRNG)
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generates cryptographically secure random bytes.
     * Uses the default SecureRandom implementation which is more secure
     * than the deprecated SHA1PRNG algorithm.
     *
     * @param n number of bytes to generate
     * @return array of random bytes
     */
    protected byte[] generateBytes(int n) {
        byte[] bytes = new byte[n];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    private byte[] mask(byte[] bytes) {
        byte[] masked = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            masked[i] = (byte) (bytes[i] & 0x7F);
        }
        return masked;
    }

    public TOTPCredentials generateCredentials() throws ServiceException {
        byte[] secretBytes = generateBytes(config.getBytesPerSecret());
        String encoded = encodeBytes(mask(secretBytes), config.getEncoding());
        List<String> scratchCodes = generateScratchCodes();
        return new TOTPCredentials(encoded, scratchCodes);
    }

    public List<String> generateScratchCodes() throws ServiceException {
        Set<String> scratchCodeSet = new HashSet<String>();
        while (scratchCodeSet.size() < config.getNumScratchCodes()) {
            scratchCodeSet.add(generateScratchCode());
        }
        List<String> scratchCodes = new ArrayList<String>(scratchCodeSet.size());
        scratchCodes.addAll(scratchCodeSet);
        return scratchCodes;
    }

    private String generateScratchCode() throws ServiceException {
        byte[] randomBytes = generateBytes(config.getBytesPerScratchCode());
        return encodeBytes(mask(randomBytes), config.getScratchCodeEncoding());
    }

    protected String encodeBytes(byte[] bytes, Encoding encoding) {
        byte[] encoded;
        switch (encoding) {
            case BASE32:
                encoded = new Base32().encode(bytes);
                return new String(encoded).toUpperCase();
            case BASE64:
                encoded = Base64.encodeBase64(bytes);
                return new String(encoded).toUpperCase();
            default:
                return null;
        }
    }
}
