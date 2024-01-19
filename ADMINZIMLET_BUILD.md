# Build Zimbra OSE 2FA Admin Zimlet

## Introduction

This is quite straight-forward to do.

## Requisites

- zip
- git

```
apt update
apt install zip git
```

## Prepare build environment

```
cd /tmp
git clone 'https://github.com/maldua-suite/zimbra-ose-2fa.git'
```

## Build

```
cd /tmp/zimbra-ose-2fa/adminZimlet/com_btactic_twofactorauth_admin/
zip --quiet -r ../com_btactic_twofactorauth_admin.zip *
```

## Zip

A new zip file should be found at:
```
/tmp/zimbra-ose-2fa/adminZimlet/com_btactic_twofactorauth_admin.zip
```
.
