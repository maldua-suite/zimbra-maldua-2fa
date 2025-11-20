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
package com.btactic.twofactorauth;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.zimbra.cs.account.auth.twofactor.TwoFactorAuth.CredentialConfig;
import com.zimbra.cs.account.auth.twofactor.ScratchCodes;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.btactic.twofactorauth.core.BaseTwoFactorAuthComponent;
import com.btactic.twofactorauth.core.TwoFactorAuthConstants;
import com.btactic.twofactorauth.core.TwoFactorAuthUtils;
import com.btactic.twofactorauth.credentials.CredentialGenerator;
import com.zimbra.cs.account.ldap.LdapLockoutPolicy;
import com.zimbra.cs.account.Provisioning;

/**
 * Manages scratch codes for two-factor authentication.
 * Scratch codes are one-time use backup codes for account recovery.
 *
 * @author iraykin
 *
 */
public class ZetaScratchCodes extends BaseTwoFactorAuthComponent implements ScratchCodes {
    private List<String> scratchCodes;
    boolean hasStoredScratchCodes;

    public ZetaScratchCodes(Account account) throws ServiceException {
        this(account, account.getName());
    }

    public ZetaScratchCodes(Account account, String acctNamePassedIn) throws ServiceException {
        super(account, acctNamePassedIn);
        TwoFactorAuthUtils.disableTwoFactorAuthIfNecessary(account);
        if (account.isFeatureTwoFactorAuthAvailable()) {
            scratchCodes = loadScratchCodes();
        }
    }

    public void clearData() throws ServiceException {
        deleteCredentials();
    }

    public CredentialConfig getCredentialConfig() throws ServiceException {
        CredentialConfig config = new CredentialConfig()
        .setSecretLength(getGlobalConfig().getTwoFactorAuthSecretLength())
        .setScratchCodeLength(getGlobalConfig().getTwoFactorScratchCodeLength())
        .setEncoding(getSecretEncoding())
        .setScratchCodeEncoding(getScratchCodeEncoding())
        .setNumScratchCodes(account.getCOS().getTwoFactorAuthNumScratchCodes());
        return config;
    }

    private List<String> loadScratchCodes() throws ServiceException {
        String encryptedCodes = account.getTwoFactorAuthScratchCodes();
        if (Strings.isNullOrEmpty(encryptedCodes)) {
            hasStoredScratchCodes = false;
            return new ArrayList<String>();
        }

        hasStoredScratchCodes = true;
        String commaSeparatedCodes = decrypt(account, encryptedCodes);
        String[] codes = commaSeparatedCodes.split(TwoFactorAuthConstants.SCRATCH_CODE_SEPARATOR);

        List<String> codeList = new ArrayList<String>(codes.length);
        for (String code : codes) {
            codeList.add(code);
        }
        return codeList;
    }

    @Override
    public void storeCodes(List<String> codes) throws ServiceException {
        String codeString = Joiner.on(TwoFactorAuthConstants.SCRATCH_CODE_SEPARATOR).join(codes);
        String encrypted = encrypt(codeString);
        account.setTwoFactorAuthScratchCodes(encrypted);
    }

    private void storeCodes() throws ServiceException {
        if (scratchCodes != null) {
            storeCodes(scratchCodes);
        }
    }

    @Override
    public List<String> generateCodes(CredentialConfig config) throws ServiceException {
        ZimbraLog.account.debug("invalidating current scratch codes");
        List<String> newCodes = new CredentialGenerator(config).generateScratchCodes();
        scratchCodes.clear();
        scratchCodes.addAll(newCodes);
        storeCodes();
        return scratchCodes;

    }


    public void authenticate(String scratchCode) throws ServiceException {
        if (!checkScratchCodes(scratchCode)) {
            failedLogin();
            ZimbraLog.account.error("invalid scratch code");
            throw AuthFailedServiceException.TWO_FACTOR_AUTH_FAILED(account.getName(), acctNamePassedIn, "invalid scratch code");
        }
    }

    public boolean checkScratchCodes(String scratchCode) throws ServiceException {
        for (String code: scratchCodes) {
            if (code.equals(scratchCode)) {
                invalidateScratchCode(code);
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> getCodes() {
        return scratchCodes;
    }

    public List<String> generateNewScratchCodes() throws ServiceException {
        ZimbraLog.account.debug("invalidating current scratch codes");
        CredentialConfig config = getCredentialConfig();
        List<String> newCodes = new CredentialGenerator(config).generateScratchCodes();
        scratchCodes.clear();
        scratchCodes.addAll(newCodes);
        storeScratchCodes();
        return scratchCodes;

    }

    private void storeScratchCodes(List<String> codes) throws ServiceException {
        String codeString = Joiner.on(TwoFactorAuthConstants.SCRATCH_CODE_SEPARATOR).join(codes);
        String encrypted = encrypt(codeString);
        account.setTwoFactorAuthScratchCodes(encrypted);
    }

    private void storeScratchCodes() throws ServiceException {
        if (scratchCodes != null) {
            storeScratchCodes(scratchCodes);
        }
    }

    private void invalidateScratchCode(String code) throws ServiceException {
        scratchCodes.remove(code);
        storeCodes();
    }

    public void deleteCredentials() throws ServiceException {
        account.setTwoFactorAuthScratchCodes(null);
    }

    private void failedLogin() throws ServiceException {
        LdapLockoutPolicy lockoutPolicy = new LdapLockoutPolicy(Provisioning.getInstance(), account);
        lockoutPolicy.failedSecondFactorLogin();
    }

}
