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

/**
 * Generates cryptographically secure credentials for two-factor authentication.
 * This class creates TOTP shared secrets and backup scratch codes using
 * SecureRandom for cryptographic strength.
 *
 * <p>Security Features:
 * <ul>
 *   <li>Uses default SecureRandom (not deprecated SHA1PRNG)</li>
 *   <li>Masks bytes to ensure compatibility with Base32/Base64 encoding</li>
 *   <li>Generates unique scratch codes (no duplicates)</li>
 *   <li>Supports configurable encoding schemes (BASE32, BASE64)</li>
 * </ul>
 *
 * @author BTACTIC
 */
public class CredentialGenerator {
    private final CredentialConfig config;
    private final SecureRandom secureRandom;

    /**
     * Creates a new credential generator with the specified configuration.
     *
     * @param config the configuration specifying secret length, encoding, etc.
     * @throws IllegalArgumentException if config is null or contains invalid values
     */
    public CredentialGenerator(CredentialConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("CredentialConfig cannot be null");
        }
        if (config.getBytesPerSecret() <= 0) {
            throw new IllegalArgumentException("Secret length must be positive, got: " + config.getBytesPerSecret());
        }
        if (config.getBytesPerScratchCode() <= 0) {
            throw new IllegalArgumentException("Scratch code length must be positive, got: " + config.getBytesPerScratchCode());
        }
        if (config.getNumScratchCodes() < 0) {
            throw new IllegalArgumentException("Number of scratch codes cannot be negative, got: " + config.getNumScratchCodes());
        }
        if (config.getEncoding() == null) {
            throw new IllegalArgumentException("Secret encoding cannot be null");
        }
        if (config.getScratchCodeEncoding() == null) {
            throw new IllegalArgumentException("Scratch code encoding cannot be null");
        }

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
     * @throws IllegalArgumentException if n is not positive
     */
    protected byte[] generateBytes(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("Number of bytes must be positive, got: " + n);
        }
        byte[] bytes = new byte[n];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    /**
     * Masks bytes by clearing the high bit to ensure compatibility with
     * Base32/Base64 encoding and to avoid character encoding issues.
     *
     * @param bytes the bytes to mask
     * @return masked bytes with high bit cleared (& 0x7F)
     */
    private byte[] mask(byte[] bytes) {
        byte[] masked = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            masked[i] = (byte) (bytes[i] & 0x7F);
        }
        return masked;
    }

    /**
     * Generates a complete set of TOTP credentials including shared secret
     * and backup scratch codes.
     *
     * @return new TOTP credentials with encoded secret and scratch codes
     * @throws ServiceException if credential generation fails
     */
    public TOTPCredentials generateCredentials() throws ServiceException {
        byte[] secretBytes = generateBytes(config.getBytesPerSecret());
        String encoded = encodeBytes(mask(secretBytes), config.getEncoding());
        List<String> scratchCodes = generateScratchCodes();
        return new TOTPCredentials(encoded, scratchCodes);
    }

    /**
     * Generates a list of unique scratch codes for backup authentication.
     * Scratch codes are one-time use codes that can be used when TOTP is unavailable.
     * Uses a Set to ensure no duplicate codes are generated.
     *
     * @return list of unique scratch codes
     * @throws ServiceException if scratch code generation fails
     */
    public List<String> generateScratchCodes() throws ServiceException {
        Set<String> scratchCodeSet = new HashSet<String>();
        while (scratchCodeSet.size() < config.getNumScratchCodes()) {
            scratchCodeSet.add(generateScratchCode());
        }
        List<String> scratchCodes = new ArrayList<String>(scratchCodeSet.size());
        scratchCodes.addAll(scratchCodeSet);
        return scratchCodes;
    }

    /**
     * Generates a single scratch code using the configured length and encoding.
     *
     * @return a single encoded scratch code
     * @throws ServiceException if scratch code generation fails
     */
    private String generateScratchCode() throws ServiceException {
        byte[] randomBytes = generateBytes(config.getBytesPerScratchCode());
        return encodeBytes(mask(randomBytes), config.getScratchCodeEncoding());
    }

    /**
     * Encodes bytes using the specified encoding scheme.
     * Supports BASE32 (Google Authenticator compatible) and BASE64.
     *
     * @param bytes the bytes to encode
     * @param encoding the encoding scheme (BASE32 or BASE64)
     * @return uppercase encoded string
     * @throws IllegalArgumentException if bytes or encoding is null, or encoding is unsupported
     */
    protected String encodeBytes(byte[] bytes, Encoding encoding) {
        if (bytes == null) {
            throw new IllegalArgumentException("Bytes to encode cannot be null");
        }
        if (encoding == null) {
            throw new IllegalArgumentException("Encoding cannot be null");
        }

        byte[] encoded;
        switch (encoding) {
            case BASE32:
                encoded = new Base32().encode(bytes);
                return new String(encoded).toUpperCase();
            case BASE64:
                encoded = Base64.encodeBase64(bytes);
                return new String(encoded).toUpperCase();
            default:
                throw new IllegalArgumentException("Unsupported encoding: " + encoding);
        }
    }
}
