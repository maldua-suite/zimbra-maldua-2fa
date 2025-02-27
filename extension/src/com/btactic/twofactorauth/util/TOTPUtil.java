package com.zimbra.cs.account.auth.twofactor;

import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.common.auth.twofactor.AuthenticatorConfig;
import com.zimbra.common.auth.twofactor.CredentialConfig.Encoding;
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
            "zmtotp [account] [secret]", null, OPTIONS, 2, 2, null);
            System.exit(0);
    }
}
