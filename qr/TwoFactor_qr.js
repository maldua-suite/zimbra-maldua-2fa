/*
 * ***** BEGIN LICENSE BLOCK *****
 * Maldua Zimbra 2FA QR
 * Copyright (C) 2023 BTACTIC, S.C.C.L.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * ***** END LICENSE BLOCK *****
 */

if(window.console && window.console.log) {
    window.console.log("Starting loading TwoFactor_qr.js");
}

// STEP 3. Update QR when Next button is clicked.
function twofactorauth_qr_setup(twofactorauth_qr_email) {
    // Receives: account's email as the only parametre

    // Default height from ZmTwoFactorSetup class is: 180px.
    // We add: 256px (QR height)
    // and an extra: 10px
    // Total: 446px

    // Classic UI
    if (! (document.querySelectorAll('.ZmTwoFactorSetup').length == 0)) {
        document.querySelectorAll('.ZmTwoFactorSetup')[0].style['height']='446px';
    }

    // Create QR Div if it is not there yet.
    if (document.querySelectorAll('#twoFactorAuthQrDiv').length == 0) {
        var emptyDivForTwoFactorAuth = document.createElement("div");
        emptyDivForTwoFactorAuth.id = 'twoFactorAuthQrDiv';
        document.querySelectorAll('.email-key')[0].after(emptyDivForTwoFactorAuth);
    }

    twofactorauth_next_button = document.querySelector("[id$=" + '_button' + String(ZmTwoFactorSetupDialog.NEXT_BUTTON) + '_title' + "]");
    twofactorauth_next_button.addEventListener('click', function(){
        document.getElementById('twoFactorAuthQrDiv').innerHTML='';
        var twofactorauth_qr_secret = document.querySelectorAll('.email-key')[0].textContent;
        var twofactorauth_qr_issuer = window.location.host; // mail.example.net
        var qrcode = new QRCode(document.getElementById('twoFactorAuthQrDiv'), {
            text: "otpauth://totp/" + twofactorauth_qr_email + "?secret=" + twofactorauth_qr_secret + "&issuer=" + twofactorauth_qr_issuer  ,
            width: 256,
            height: 256,
            colorDark : "#000000",
            colorLight : "#ffffff",
            correctLevel : QRCode.CorrectLevel.H
        });
    }
    );

}

// STEP 2. Default 2FA listener override with our Setup function
ZmTwoFactorSetupDialog.prototype._beginSetupButtonListener = (function(_super) {
    return function() {
        twofactorauth_qr_setup(this.username);
        arguments[0] = arguments[0] - 1;
        return _super.apply(this, arguments);
    };

})(ZmTwoFactorSetupDialog.prototype._beginSetupButtonListener);
