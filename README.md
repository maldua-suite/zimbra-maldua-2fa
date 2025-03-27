# Zimbra OSE 2FA

![Zimbra 2FA Splash](images/zimbra-maldua-2fa-splash.png)

## About

**MALDUA'S Zimbra OSE 2FA Extension & Administration Zimlet** brought to you by [BTACTIC, open source & cloud solutions](https://www.btactic.com).

Two-factor authentication adds an additional layer of security to your Zimbra login.
Thanks to a third-party authenticator such as Google Authenticator Zimbra users are now required to enter a randomly generated code.

## Supported Zimbra versions

- Zimbra 10.1.x

Please note that for Zimbra 8.8.15, Zimbra 9.0.0 and Zimbra 10.0.x you can use zimbra-maldua-2fa v0.8.0.

## Non support

- Z-Push: Please notice that currently Z-Push is not supported by this extension. You will only be able to use Z-Push with an account if you disable 2FA for that specific account. Please check [Z-Push using Application Passcode is not working as expected](https://github.com/maldua-suite/zimbra-maldua-2fa/issues/7) for more updated information.

- Whitelist IPs: Whitelist internal IPs so that they are not asked for 2FA is not supported. Check: **Trusted devices** instead.

## Features

### Integrated with Zimbra Webclient UI

Seamless integrated with native Zimbra Webclient UI for 2FA.

![Setup two-step authentication ...](images/twofactorauthentication-webclient1.png)

![Setup two-step authentication wizard](images/twofactorauthentication-webclient2.png)

### Includes QR support

![Setup two-step authentication wizard with QR](images/twofactorauthentication-webclient9.png)

### Basic 2FA

An additional authentication factor based on TOTP (Time-based One-Time Passwords). This is compatible with Google Authenticator or Authy.

![Verify step](images/twofactorauthentication-verify.png)

### Email 2FA

An additional authentication factor based on sending a code to a secondary email address. No need to use a TOTP app.

**Please note** that when Two Factor Authentication is required (mandatory), email method has been configured but app method has not been configured, recovery address cannot be changed because it is used for Two Factor Authentication and Two Factor Authentication email method cannot be disabled.

![Email based 2FA Login](images/twofactorauthentication-email1.png)

### Trusted devices

Mark your usual device as trusted so that you are not asked for 2FA each time you login.

### Application specific passwords

Do you have **Imap** or pop3 applications that do not support 2FA?
Keep using them with an specific password for each one of them.

![Application name](images/twofactorauthentication-application1.png)
![Application passcode](images/twofactorauthentication-application2.png)
![Applications in Webclient](images/twofactorauthentication-application3.png)

### Scratch codes

Scratch or one-time use codes are generated so that you can write them down in a paper just in case your 2FA application no longer works for you.

![Scratch codes in Webclient](images/twofactorauthentication-scratch1.png)
![Scratch popup](images/twofactorauthentication-scratch2.png)

### Network Edition binary compatibility upgrade

Both *Zimbra OSE 2FA* and current *Zimbra Network Edition* share a design based on a public codebase from around 2016.

Take a look at this scenario:

- ZCS OSE 8.8.15 - **Standard ZCS OSE 8.8.15**
- ZCS OSE 8.8.15 + zimbra-maldua-2fa - **zimbra-maldua-2fa is installed**
- Enable/**Use 2FA** features in different Classes of Services or accounts.
- ZCS OSE 8.8.15 - **Uninstall zimbra-maldua-2fa**
- ZCS NE 8.8.15 - **Upgrade from ZCS OSE to ZCS NE**

Once you have upgraded to ZCS NE 8.8.15 all of the 2FA features that were enabled/used in **ZCS OSE 8.8.15 + zimbra-maldua-2fa** setup should keep working. No need to reissue 2FA codes and ask final users to update their Google Authenticator, Authy or specific Thunderbird/Imap client password.

## Admin documentation

### Basic Management

When creating or editing a class of service or an account there is an additional tab named **2FA (Maldua)** where you can:

- Enable or disable 2FA feature
- Allow or Disallow:
    - Application 2FA method
    - Email 2FA method
- Enable or disable:
    - Application 2FA method
    - Email 2FA method
- Check if the user has activated 2FA (Only available in accounts)
- Check if the account requires 2FA for login
- Enable or disable application specific passwords or passcodes
- Setup the numer of scratch codes to generate
- Choose a primary 2FA method
- Set Email code lifetime
- Disable App Two Factor Authentication Method
- Disable Email Two Factor Authentication Method

![Admin Zimlet for Two Factor Authentication](images/twofactorauthentication-adminzimlet1.png)

### More Email Settings

At Global Settings or at a domain level you can:

- Set Allowed Methods
- Set Email code lifetime
- Change Subject and Body of 2FA Email Code template

![Extra Admin Settings for 2FA Email](images/twofactorauthentication-adminzimlet2.png)

### Disabling 2FA for an user

When disabling 2FA for an user you need to make sure to:

- Uncheck **Active**
- Uncheck **Require 2FA**
.


Otherwise the user will be asked for the 2FA code when loging in if he has ever setup 2FA in the past.

## CLI commands

### zetatotp

Given an account and its secret you can generate its TOTP code without the need of a Google Authenticator app.
This is useful is, as an admin, you don't want to install a Google Authenticator app yourself but you want to try how the feature works.

```
Usage:
zetatotp --account ACCOUNT --secret SECRET

Example:
zetatotp --account name@example.net --secret ASE34553
Current TOTP code is: 436244
```

### Extra documentation

In addition to the documentation you can find in this README you should be also checking:

- [Zimbra Wiki - Two-factor authentication](https://wiki.zimbra.com/wiki/Zimbra_Two-factor_authentication)
- [Zimbra Blog - Did You Know? Zimbra Two-Factor Authentication (2FA)](https://blog.zimbra.com/2022/03/did-you-know-zimbra-two-factor-authentication-2fa/)
- [Zimbra Tips & Tricks - Enabling Two-Factor Authentication (2FA) Video](https://www.youtube.com/watch?v=_eEwnnaEvMU)

.

Not everything described there applies to this Open Source implementation but it can be helpful to understand how the technology works.

### Upgrade

#### From 0.7.0 version

If you have installed 0.7.0 version please run as root:
```
cp /opt/zimbra/jetty/webapps/zimbra/public/TwoFactorSetup.jsp_2FAQR_COPY /opt/zimbra/jetty/webapps/zimbra/public/TwoFactorSetup.jsp
cp /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js_2FAQR_COPY /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js
```
before running the installation.

#### From 0.6.0 version

If you have installed 0.6.0 version please uninstall the QR zimlet with:

```
su - zimbra -c 'zmzimletctl undeploy com_btactic_twofactorauth_qr'
```

because you won't need it anymore.

### Installation

#### Time sync warning

As per Zimbra installation requisites your Operating System should have its time in sync with global clocks thanks to tools such as:

- ntpd
- [systemd-timesyncd](https://wiki.archlinux.org/title/systemd-timesyncd)

otherwise the final user might get a wrong password even if it's the correct one.

Please notice that the device where Google Authenticator is needs its time to be in sync too.

Finally if you ever need it you can check `zimbraTwoFactorTimeWindowOffset` attribute described on [Steps to fix 2FA setup error - Zimbra Wiki](https://wiki.zimbra.com/wiki/Steps_to_fix_two_factor_auth_setup_error).

#### Automatic installation

**Notice:** In a Multi-Server cluster these commands have to be run on each one of the mailbox nodes.

```
sudo -i # Become root
cd /tmp
wget 'https://github.com/maldua-suite/zimbra-maldua-2fa/releases/download/v0.9.3/zimbra-maldua-2fa_0.9.3.tar.gz'
tar xzf zimbra-maldua-2fa_0.9.3.tar.gz
cd zimbra-maldua-2fa_0.9.3
```

For regular installation or upgrade you can run:
```
./install.sh
```
instead
.

In order for the two-factor authentication extension and the adminZimlet to apply you need to restart mailboxd with:
```
sudo -i # Become root
su - zimbra -c 'zmmailboxdctl restart'
```

#### Manual installation

**Notice:** In a Multi-Server cluster these commands have to be run on each one of the mailbox nodes.

**WARNING:** Please change **0.9.3** with whatever it's the latest released version.

```
sudo -i # Become root
cd /tmp
wget 'https://github.com/maldua-suite/zimbra-maldua-2fa/releases/download/v0.9.3/zimbra-maldua-2fa_0.9.3.tar.gz'
tar xzf zimbra-maldua-2fa_0.9.3.tar.gz
chown zimbra:zimbra zimbra-maldua-2fa_0.9.3
chown zimbra:zimbra zimbra-maldua-2fa_0.9.3/com_btactic_twofactorauth_admin.zip
cd zimbra-maldua-2fa_0.9.3
cp zetatwofactorauth.jar /opt/zimbra/lib/ext/twofactorauth/zetatwofactorauth.jar
su - zimbra -c 'zmzimletctl -l deploy /tmp/zimbra-maldua-2fa_0.9.3/com_btactic_twofactorauth_admin.zip'

chown zimbra:zimbra qr
chown zimbra:zimbra qr/qrcode.js
chown zimbra:zimbra qr/TwoFactor_qr.js

cp qr/qrcode.js /opt/zimbra/jetty/webapps/zimbra/js
cp qr/TwoFactor_qr.js /opt/zimbra/jetty/webapps/zimbra/js
chown zimbra:zimbra /opt/zimbra/jetty/webapps/zimbra/js/qrcode.js
chown zimbra:zimbra /opt/zimbra/jetty/webapps/zimbra/js/TwoFactor_qr.js
su - zimbra -c 'cat /opt/zimbra/jetty/webapps/zimbra/js/qrcode.js | gzip -c > /opt/zimbra/jetty/webapps/zimbra/js/qrcode.js.zgz'
su - zimbra -c 'cat /opt/zimbra/jetty/webapps/zimbra/js/TwoFactor_qr.js | gzip -c > /opt/zimbra/jetty/webapps/zimbra/js/TwoFactor_qr.js.zgz'

cp /opt/zimbra/jetty/webapps/zimbra/public/TwoFactorSetup.jsp /opt/zimbra/jetty/webapps/zimbra/public/TwoFactorSetup.jsp_2FAQR_COPY
sed -i 's~</head>~<script src="${contextPath}/js/qrcode.js<%=ext%>?v=${version}"></script><script src="${contextPath}/js/TwoFactor_qr.js<%=ext%>?v=${version}"></script></head>~g' /opt/zimbra/jetty/webapps/zimbra/public/TwoFactorSetup.jsp

cp /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js_2FAQR_COPY
cp /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js.zgz /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js.zgz_2FAQR_COPY
cat /opt/zimbra/jetty/webapps/zimbra/js/qrcode.js >> /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js
cat /opt/zimbra/jetty/webapps/zimbra/js/TwoFactor_qr.js >> /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js
su - zimbra -c 'cat /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js | gzip -c > /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js.zgz'
```

In order for the two-factor authentication extension and the adminZimlet to apply you need to restart mailboxd with:
```
sudo -i # Become root
su - zimbra -c 'zmmailboxdctl restart'
```

### Uninstallation

```
sudo -i # Become root
su - zimbra -c 'zmzimletctl undeploy com_btactic_twofactorauth_admin'
mv /opt/zimbra/lib/ext/twofactorauth/zetatwofactorauth.jar /root/zetatwofactorauth.jar-REMOVED-ON-YYYY-MM-DD
cp /opt/zimbra/jetty/webapps/zimbra/public/TwoFactorSetup.jsp_2FAQR_COPY /opt/zimbra/jetty/webapps/zimbra/public/TwoFactorSetup.jsp
cp /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js_2FAQR_COPY /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js
cp /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js.zgz_2FAQR_COPY /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js.zgz

mkdir /root/zetaqraddon-REMOVED-ON-YYYY-MM-DD

mv /opt/zimbra/jetty/webapps/zimbra/js/qrcode.js /root/zetaqraddon-REMOVED-ON-YYYY-MM-DD
mv /opt/zimbra/jetty/webapps/zimbra/js/TwoFactor_qr.js /root/zetaqraddon-REMOVED-ON-YYYY-MM-DD
mv /opt/zimbra/jetty/webapps/zimbra/js/qrcode.js.zgz /root/zetaqraddon-REMOVED-ON-YYYY-MM-DD
mv /opt/zimbra/jetty/webapps/zimbra/js/TwoFactor_qr.js.zgz /root/zetaqraddon-REMOVED-ON-YYYY-MM-DD

mv /opt/zimbra/jetty/webapps/zimbra/public/TwoFactorSetup.jsp_2FAQR_COPY /root/zetaqraddon-REMOVED-ON-YYYY-MM-DD
mv /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js_2FAQR_COPY /root/zetaqraddon-REMOVED-ON-YYYY-MM-DD
mv /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js.zgz_2FAQR_COPY /root/zetaqraddon-REMOVED-ON-YYYY-MM-DD
```

In order for the removal to be applied you need to restart mailboxd with:
```
sudo -i # Become root
su - zimbra -c 'zmmailboxdctl restart'
```
.

### Additional notes

The QR addon modifies some stock Zimbra files.

Those are:

- /opt/zimbra/jetty/webapps/zimbra/public/TwoFactorSetup.jsp
- /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js
- /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js.zgz

.

Automatic installation makes copies of those files here:

- /opt/zimbra/jetty/webapps/zimbra/public/TwoFactorSetup.jsp_2FAQR_COPY
- /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js_2FAQR_COPY
- /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js.zgz_2FAQR_COPY

.

## Known bugs

- When a domain is edited you can find additional buttons in the *2FA (Maldua)* tab for uploading Domain certificate and its private certificate. This is actually a `zm-certificate-manager-admin-zimlet`: [https://github.com/Zimbra/zm-certificate-manager-admin-zimlet/pull/8] bug that it has been reported in 2025 so that **Zimbra fixes it hopefully in 2028**.

## Developer documentation

This documentation is aimed at developers, not at admins.

### How to build the extension

- Check: [EXTENSION_BUILD.md](EXTENSION_BUILD.md) on how to build the Extension.

### How to install the extension

- Check: [EXTENSION_INSTALL.md](EXTENSION_INSTALL.md) on how to install the Extension.

### How to build the admin zimlet

- Check: [ADMINZIMLET_BUILD.md](ADMINZIMLET_BUILD.md) on how to build the Administration Console Zimlet.

### How to install the admin zimlet

- Check: [ADMINZIMLET_INSTALL.md](ADMINZIMLET_INSTALL.md) on how to install the Administration Console Zimlet.

### How to release the extension and admin zimlet

- Check: [RELEASE.md](RELEASE.md) on how to release the extension and admin zimlet.

## Some background

This is some background for those of you that enjoy reading developer stories.

At the [Zimbra Roadmap and Product Update from February, 2015](https://cdn2.hubspot.net/hub/212115/file-2452880015-pdf/pdf_files/2015_Roadmap_Update_-_Feb_2015_FINAL.pdf) you can read about how for ZCS 8.7 there was a Mobile Gateway section that mentioned: *Zimbra Mobile Gateway + Push Notifications + 2-Factor Security*.

This was actually ZCS 8.6 being improved for having such features.

Development versions of ZCS OSE 8.6 had an initial implementation of 2FA but, then, someone at Zimbra, decided that it was worth it moving it to the NE version as an extension (2FA was not going to be available at OSE version!). More over the 2FA webclient support will be refactored in such a way so that alternative 2FA implementations could be written by other developers or companies.

You can take a look at commits from those days:

- [zm-mailbox-zmg-2fa's zmg-2fa-last-snapshot](https://github.com/adriangibanelbtactic/zm-mailbox-zmg-2fa/tree/zmg-2fa-last-snapshot)
- [zm-mailbox-zmg-2fa's zmg-2fa-last-soap-snapshot](https://github.com/adriangibanelbtactic/zm-mailbox-zmg-2fa/tree/zmg-2fa-last-soap-snapshot)
- [zm-mailbox-zmg-2fa' zmg-2fa-move-to-ne-snapshot](https://github.com/adriangibanelbtactic/zm-mailbox-zmg-2fa/tree/zmg-2fa-move-to-ne-snapshot)

So... this extension is an affirmative answer to this question...

**Is it possible to rewrite the old 8.6 code for 2FA so that it can be ported into its own extension?**

## Licenses

### License (Extension)

```
Zimbra OSE 2FA Extension
Copyright (C) 2023 BTACTIC, S.C.C.L.

Zimbra Collaboration Suite Server
Copyright (C) 2007, 2008, 2009, 2010, 2013, 2014 Zimbra, Inc.

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software Foundation,
version 2 of the License.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with this program.
If not, see <http://www.gnu.org/licenses/>.
```

### License (Administration zimlet)

```
Zimbra OSE 2FA Administration zimlet
Copyright (C) 2023 BTACTIC, S.C.C.L.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see http://www.gnu.org/licenses/.
```

### License (QR Addon)

```
Zimbra OSE 2FA QR Addon
Copyright (C) 2023 BTACTIC, S.C.C.L.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see http://www.gnu.org/licenses/.
```

### License (QRJS library)

```
The MIT License (MIT)
---------------------
Copyright (c) 2012 davidshimjs

Permission is hereby granted, free of charge,
to any person obtaining a copy of this software and associated
 documentation files (the "Software"),
to deal in the Software without restriction,
including without limitation the rights to use, copy,
 modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons
 to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall
 be included in all copies or substantial portions
 of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```
