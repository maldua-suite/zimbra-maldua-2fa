# Install Zimbra OSE 2FA Extension

## Warning

**These are developer instructions.**

For Admin installation instructions please check [README.md](README.md) instead.

## Requisites

You have succesfully built zimbra-maldua-2fa using [EXTENSION_BUILD.md](EXTENSION_BUILD.md) instructions.

## Installation

Get `/opt/zimbra/conf/scripts/zimbra-maldua-2fa/extension/zetatwofactorauth.jar` from your build machine and copy it to your production machine on `/tmp/zetatwofactorauth.jar` .

This needs to be run as the root user:

```
cp /tmp/zetatwofactorauth.jar /opt/zimbra/lib/ext/twofactorauth/zetatwofactorauth.jar
```

## Zimbra Mailbox restart

For the new Extension to be used the Zimbra Mailbox has to be restarted.

```
sudo su - zimbra -c 'zmmailboxdctl restart'
```

## Network Edition notes

This is not supposed to work in a Zimbra NE installation.
If you insist on using this extension in a Zimbra NE installation please move the original `/opt/zimbra/lib/ext/twofactorauth/zimbratwofactorauth.jar` file from Zimbra NE somewhere else so that they do not collide.
