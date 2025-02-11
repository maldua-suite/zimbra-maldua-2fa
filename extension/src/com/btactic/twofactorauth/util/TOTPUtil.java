/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra OSE 2FA Administration zimlet
 * Copyright (C) 2025 BTACTIC, S.C.C.L.
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
package com.btactic.twofactorauth.util;

import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.common.auth.twofactor.AuthenticatorConfig;
import com.zimbra.common.auth.twofactor.TwoFactorOptions.Encoding;
import com.zimbra.common.auth.twofactor.TOTPAuthenticator;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

public class TOTPUtil {

    private static Options OPTIONS = new Options();

    static {
        OPTIONS.addOption("s", "secret", true,  "Shared secret");
        OPTIONS.addOption("a", "account", true, "Account name");
        OPTIONS.addOption("h", "help", false, "Display this help message");
    }

    public static void main(String[] args) throws ParseException, ServiceException {
        CliUtil.toolSetup();
        CommandLineParser parser = new GnuParser();
        CommandLine cl = parser.parse(OPTIONS, args);
        Provisioning prov = Provisioning.getInstance();
        if (cl.hasOption("h") || !cl.hasOption("a") || !cl.hasOption("s")) {
            usage();
            return;
        }
        String acctName = cl.getOptionValue("a");
        String secret = cl.getOptionValue("s");;
        Account acct = prov.getAccountByName(acctName);
        TwoFactorManager manager = new TwoFactorManager(acct);
        AuthenticatorConfig config = manager.getAuthenticatorConfig();
        TOTPAuthenticator authenticator = new TOTPAuthenticator(config);
        Encoding encoding = manager.getCredentialConfig().getEncoding();
        Long timestamp = System.currentTimeMillis() / 1000;
        String code = authenticator.generateCode(secret, timestamp, encoding);
        System.out.println("Current TOTP code is: " + code);
    }

    private static void usage() {
        HelpFormatter format = new HelpFormatter();
        format.printHelp(new PrintWriter(System.err, true), 80,
            "zetatotp [account] [secret]", null, OPTIONS, 2, 2, null);
            System.exit(0);
    }
}
