#!/bin/bash

function deploy_qr_addon () {

# Set proper users and groups
chown zimbra:zimbra qr
chown zimbra:zimbra qr/qrcode.js
chown zimbra:zimbra qr/TwoFactor_qr.js

# Login 2FA
cp qr/qrcode.js /opt/zimbra/jetty/webapps/zimbra/js
cp qr/TwoFactor_qr.js /opt/zimbra/jetty/webapps/zimbra/js
chown zimbra:zimbra /opt/zimbra/jetty/webapps/zimbra/js/qrcode.js
chown zimbra:zimbra /opt/zimbra/jetty/webapps/zimbra/js/TwoFactor_qr.js
su - zimbra -c 'cat /opt/zimbra/jetty/webapps/zimbra/js/qrcode.js | gzip -c > /opt/zimbra/jetty/webapps/zimbra/js/qrcode.js.zgz'
su - zimbra -c 'cat /opt/zimbra/jetty/webapps/zimbra/js/TwoFactor_qr.js | gzip -c > /opt/zimbra/jetty/webapps/zimbra/js/TwoFactor_qr.js.zgz'

if grep -E 'TwoFactor_qr.js' /opt/zimbra/jetty/webapps/zimbra/public/TwoFactorSetup.jsp > /dev/null 2>&1 ; then
    :
else
    cp /opt/zimbra/jetty/webapps/zimbra/public/TwoFactorSetup.jsp /opt/zimbra/jetty/webapps/zimbra/public/TwoFactorSetup.jsp_2FAQR_COPY
    sed -i 's~</head>~<script src="${contextPath}/js/qrcode.js<%=ext%>?v=${version}"></script><script src="${contextPath}/js/TwoFactor_qr.js<%=ext%>?v=${version}"></script></head>~g' /opt/zimbra/jetty/webapps/zimbra/public/TwoFactorSetup.jsp
fi

# Preferences 2FA
if grep -E 'TwoFactor_qr.js' /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js > /dev/null 2>&1 ; then
    :
else
    cp /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js_2FAQR_COPY
    cp /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js.zgz /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js.zgz_2FAQR_COPY
    cat /opt/zimbra/jetty/webapps/zimbra/js/qrcode.js >> /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js
    cat /opt/zimbra/jetty/webapps/zimbra/js/TwoFactor_qr.js >> /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js
    su - zimbra -c 'cat /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js | gzip -c > /opt/zimbra/jetty/webapps/zimbra/js/Preferences_all.js.zgz'
fi

}

function usage () {

cat << EOF

$0
Regular installation.

$0 --help
Print this help.

EOF

}

function restart_notice () {

cat << EOF

- Zimbra 2FA Extension
- Zimbra 2FA Admin zimlet
- Zimbra 2FA QR addon
were installed.

Please restart mailboxd thanks to:

su - zimbra -c 'zmmailboxdctl restart'

so that this new extension is used.
EOF

}

if [ "$EUID" -ne 0 ]
  then echo "Please run as root"
  exit 1
fi

if [[ "$1" == '--help' ]]
then
    usage
    exit 0
fi

if [[ ! -x /opt/zimbra/bin/zmmailboxdctl ]]
  then echo "Please run on a mailbox node."
  exit 1
fi

chown zimbra:zimbra $(pwd)
chown zimbra:zimbra com_btactic_twofactorauth_admin.zip

cp zetatwofactorauth.jar /opt/zimbra/lib/ext/twofactorauth/zetatwofactorauth.jar
chmod 444 /opt/zimbra/lib/ext/twofactorauth/zetatwofactorauth.jar
su - zimbra -c 'zmzimletctl -l deploy '"$(pwd)"'/com_btactic_twofactorauth_admin.zip'
deploy_qr_addon

restart_notice
