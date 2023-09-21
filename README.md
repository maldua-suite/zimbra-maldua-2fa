# Zimbra OSE 2FA

![Zimbra 2FA Splash](images/zimbra-ose-2fa-splash.png)

## About

**MALDUA'S Zimbra OSE 2FA Extension & Administration Zimlet** brought to you by [BTACTIC, open source & cloud solutions](https://www.btactic.com).

Two-factor authentication adds an additional layer of security to your Zimbra login.
Thanks to a third-party authenticator such as Google Authenticator Zimbra users are now required to enter a randomly generated code.

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
- ZCS OSE 8.8.15 + zimbra-ose-2fa - **zimbra-ose-2fa is installed**
- Enable/**Use 2FA** features in different Classes of Services or accounts.
- ZCS OSE 8.8.15 - **Uninstall zimbra-ose-2fa**
- ZCS NE 8.8.15 - **Upgrade from ZCS OSE to ZCS NE**

Once you have upgraded to ZCS NE 8.8.15 all of the 2FA features that were enabled/used in **ZCS OSE 8.8.15 + zimbra-ose-2fa** setup should keep working. No need to reissue 2FA codes and ask final users to update their Google Authenticator, Authy or specific Thunderbird/Imap client password.

## Admin documentation

### Management

When creating or editing a class of service or an account there is an additional tab named **2FA (Maldua)** where you can:

- Enable or disable 2FA feature
- Check if the user has activated 2FA (Only available in accounts)
- Check if the account requires 2FA for login
- Enable or disable application specific passwords or passcodes
- Setup the numer of scratch codes to generate

![Admin Zimlet for Two Factor Authentication](images/twofactorauthentication-adminzimlet1.png)

### Extra documentation

In addition to the documentation you can find in this README you should be also checking:

- [Zimbra Wiki - Two-factor authentication](https://wiki.zimbra.com/wiki/Zimbra_Two-factor_authentication)
- [Zimbra Blog - Did You Know? Zimbra Two-Factor Authentication (2FA)](https://blog.zimbra.com/2022/03/did-you-know-zimbra-two-factor-authentication-2fa/)
- [Zimbra Tips & Tricks - Enabling Two-Factor Authentication (2FA) Video](https://www.youtube.com/watch?v=_eEwnnaEvMU)

.

Not everything described there applies to this Open Source implementation but it can be helpful to understand how the technology works.

### Installation

In a Multi-Server cluster these commands have to be run on each one of the mailbox nodes.

**Option A: Automatic installation**

```
sudo -i # Become root
cd /tmp
wget 'https://github.com/btactic/zimbra-ose-2fa/releases/download/v0.6.0/zimbra-ose-2fa_0.6.0.tar.gz'
tar xzf zimbra-ose-2fa_0.6.0.tar.gz
cd zimbra-ose-2fa_0.6.0
```

If you want to ensure that the QR zimlet is compulsory in all of the CoS (**recommended on the first installation**) run:
```
./install.sh --compulsory
```
.

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

**Option B: Manual installation**

**WARNING:** Please change **0.6.0** with whatever it's the latest released version.

```
sudo -i # Become root
cd /tmp
wget 'https://github.com/btactic/zimbra-ose-2fa/releases/download/v0.6.0/zimbra-ose-2fa_0.6.0.tar.gz'
tar xzf zimbra-ose-2fa_0.6.0.tar.gz
chown zimbra:zimbra zimbra-ose-2fa_0.6.0
chown zimbra:zimbra zimbra-ose-2fa_0.6.0/com_btactic_twofactorauth_admin.zip
chown zimbra:zimbra zimbra-ose-2fa_0.6.0/com_btactic_twofactorauth_qr.zip
cd zimbra-ose-2fa_0.6.0
cp zetatwofactorauth.jar /opt/zimbra/lib/ext/twofactorauth/zetatwofactorauth.jar
su - zimbra -c 'zmzimletctl -l deploy /tmp/zimbra-ose-2fa_0.6.0/com_btactic_twofactorauth_admin.zip'
su - zimbra -c 'zmzimletctl -l deploy /tmp/zimbra-ose-2fa_0.6.0/com_btactic_twofactorauth_qr.zip'
```

In order for the two-factor authentication extension and the adminZimlet to apply you need to restart mailboxd with:
```
sudo -i # Become root
su - zimbra -c 'zmmailboxdctl restart'
```

As an additional step make sure that the QR zimlet is compulsory in all of the CoS you want to.

### Uninstallation

```
sudo -i # Become root
su - zimbra -c 'zmzimletctl undeploy com_btactic_twofactorauth_admin'
su - zimbra -c 'zmzimletctl undeploy com_btactic_twofactorauth_qr'
mv /opt/zimbra/lib/ext/twofactorauth/zetatwofactorauth.jar /root/zetatwofactorauth.jar-REMOVED-ON-YYYY-MM-DD
```

In order for the removal to be applied you need to restart mailboxd with:
```
sudo -i # Become root
su - zimbra -c 'zmmailboxdctl restart'
```
.

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

### How to build the QR zimlet

- Check: [ZIMLET_BUILD.md](ZIMLET_BUILD.md) on how to build the QR Zimlet.

### How to install the QR zimlet

- Check: [ZIMLET_INSTALL.md](ZIMLET_INSTALL.md) on how to install the QR Zimlet.

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

### License (QR zimlet)

```
Zimbra OSE 2FA QR Zimlet
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
