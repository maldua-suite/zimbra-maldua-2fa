#!/bin/bash

VERSION="$(head -n 1 VERSION)"
ZIP_DIR="zimbra-maldua-2fa_${VERSION}"
VERSION_TEMPLATE="9.99.999"

# Build extension
cd extension
ant jar
cd ..

# Build admin zimlet
cd adminZimlet/com_btactic_twofactorauth_admin
sed -i 's/'"${VERSION_TEMPLATE}"'/'"${VERSION}"'/g' com_btactic_twofactorauth_admin.xml
zip --quiet -r ../com_btactic_twofactorauth_admin.zip *
cd ../..

# Zip directory
mkdir release/${ZIP_DIR}
cp extension/zetatwofactorauth.jar release/${ZIP_DIR}/zetatwofactorauth.jar
cp adminZimlet/com_btactic_twofactorauth_admin.zip release/${ZIP_DIR}/com_btactic_twofactorauth_admin.zip
cp -r qr release/${ZIP_DIR}/qr
cp install.sh release/${ZIP_DIR}/install.sh

# Zip file
cd release
tar czf ${ZIP_DIR}.tar.gz ${ZIP_DIR}
cd ..
