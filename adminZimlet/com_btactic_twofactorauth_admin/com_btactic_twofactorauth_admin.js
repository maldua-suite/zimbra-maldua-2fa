/*
 * ***** BEGIN LICENSE BLOCK *****
 * Maldua Zimbra 2FA Extension
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

if(ZaSettings && ZaSettings.EnabledZimlet["com_btactic_twofactorauth_admin"]){

    function com_btactic_twofactorauth_ext () {

    }

    if (window.console && console.log) {
        console.log("Start loading com_btactic_twofactorauth_admin.js");
    }

    // Inspired on checkInteropSettings function from ZaItem.js
    com_btactic_twofactorauth_admin.disableMethod =
    function (method) {

        var controller =  ZaApp.getInstance().getCurrentController() ;
        var controllerAccount = controller._currentObject;

        try {

          var soapCmd  = "DisableTwoFactorAuthRequest";
          var soapDoc = AjxSoapDoc.create(soapCmd, ZaZimbraAdmin.URN, null);
          var soapMethod  = soapDoc.set("method", method) ;
          var soapAccount = soapDoc.set("account", controllerAccount.id);
          var soapByID = soapAccount.setAttribute("by", "id");

          var params = new Object();
          params.soapDoc = soapDoc;
          var reqMgrParams = {
              controller : controller ,
              busyMsg : "Disabling TwoFactorAuth for " + method + " method."
          }
          var resp = ZaRequestMgr.invoke(params, reqMgrParams).Body.DisableTwoFactorAuthResponse;
          ZaZimbraAdmin.prototype._refreshListener.call(ZaZimbraAdmin.getInstance())

          // TODO: Disable button
        } catch (e) {
            controller._handleException(e)  ;
        }
    }

    // Inspired from ZaModel.getBooleanChoices
    // TODO: Use translations instead of hardcoded strings
    ZaModel.getZetaTwoFactorAuthMethodChoices = function () {
        return [
            {
                value: "app",
                label: com_btactic_twofactorauth_admin.lbl_app
            },
            {
                value: "email",
                label: com_btactic_twofactorauth_admin.lbl_email
            }
        ];
    }

    ZaModel.ZETA_TWOFACTORAUTH_METHOD_CHOICES= ZaModel.getZetaTwoFactorAuthMethodChoices ;

    com_btactic_twofactorauth_ext.getFirstSwitchPosition = function (xFormObject) {
        var cnt = xFormObject.items.length;
        var i = 0;
        for(i = 0; i <cnt; i++) {
            if(xFormObject.items[i].type=="switch")
                break;
        }

        return i;
    }

    com_btactic_twofactorauth_ext.viewAppButtonThis = '';

    com_btactic_twofactorauth_ext.viewAppButton = function() {
        // Hardcode number of methods: 2
        // App button: 0
        // Email button: 1

        com_btactic_twofactorauth_ext.viewAppButtonThis = this;

        var enableCheckboxId = com_btactic_twofactorauth_ext.enableCheckboxId;
        var isEnabled = true;
        if (enableCheckboxId != '') {
            var app_button_ix = 0;
            var email_button_ix = 1;
            for (var i = 0; i < 2; i++) {
                var checkbox = document.getElementById(enableCheckboxId + "_choiceitem_" + i);
                if (checkbox) {

                  checkbox.disabled = true;
                  if ( i == app_button_ix ) {
                      if (!(checkbox.checked)) {
                          isEnabled = false;
                      }
                  }
                }
            }
        }
        return (isEnabled);
    }

    com_btactic_twofactorauth_ext.viewEmailButtonThis = '';

    com_btactic_twofactorauth_ext.viewEmailButton = function() {
        // Hardcode number of methods: 2
        // App button: 0
        // Email button: 1

        com_btactic_twofactorauth_ext.viewEmailButtonThis = this;

        var enableCheckboxId = com_btactic_twofactorauth_ext.enableCheckboxId;
        var isEnabled = true;
        if (enableCheckboxId != '') {
            var app_button_ix = 0;
            var email_button_ix = 1;
            for (var i = 0; i < 2; i++) {
                var checkbox = document.getElementById(enableCheckboxId + "_choiceitem_" + i);
                if (checkbox) {

                  checkbox.disabled = true;
                  if ( i == email_button_ix ) {
                      if (!(checkbox.checked)) {
                          isEnabled = false;
                      }
                  }
                }
            }
        }
        return (isEnabled);
    }

    com_btactic_twofactorauth_ext.disableEnableMethods = function() {
        // It only makes sense to update the disable buttons when this function is called
        // for the second time (__disableEnableMethodsON variable keeps track of it)
        // because then the checkboxes have been created.
        //
        // This is in addition to the regular viewAppButton and viewEmailButton functions
        // that implement enableDisableChecks.

        var enableCheckboxId = this.getId();
        com_btactic_twofactorauth_ext.enableCheckboxId = enableCheckboxId;

        // Hardcode number of methods: 2
        // App button: 0
        // Email button: 1
        var app_button_ix = 0;
        var email_button_ix = 1;
            for (var i = 0; i < 2; i++) {
                var checkbox = document.getElementById(enableCheckboxId + "_choiceitem_" + i);
                if (checkbox) {

                  checkbox.disabled = true;
                  if ( (this.__disableEnableMethodsON) && ( i == app_button_ix ) ) {
                      if (!(checkbox.checked)) {
                          com_btactic_twofactorauth_ext.viewAppButtonThis.disableElement();
                      } else {
                          com_btactic_twofactorauth_ext.viewAppButtonThis.enableElement();
                      }
                  }
                  if ( (this.__disableEnableMethodsON) && ( i == email_button_ix ) ) {
                      if (!(checkbox.checked)) {
                          com_btactic_twofactorauth_ext.viewEmailButtonThis.disableElement();
                      } else {
                          com_btactic_twofactorauth_ext.viewEmailButtonThis.enableElement();
                      }
                  }
                }
            }

        if (!this.__disableEnableMethodsON) {
          this.__disableEnableMethodsON = true;
        }

    }

    com_btactic_twofactorauth_ext.initDisableEnableMethods = function () {
        if (!this.__initDisableEnableMethodsON) {
          this.__initDisableEnableMethodsON = true;
          this.items[0].items[0].setElementEnabled = com_btactic_twofactorauth_ext.disableEnableMethods;
        }
        return false;
    }

    com_btactic_twofactorauth_ext.viewEmailFrom = function() {
        return false;
    }

    // Using getResource from a ZmZimletBase object does not seem to work in admin
    com_btactic_twofactorauth_admin.zimletImagesPath = "/service/zimlet/com_btactic_twofactorauth_admin/images"


    com_btactic_twofactorauth_admin.malduaHeader =
      '<a target="_blank" href="https://github.com/maldua-suite/maldua-suite">' +
      '<img align="right" alt="Maldua Suite for Zimbra Collaboration Server" src="' +
      com_btactic_twofactorauth_admin.zimletImagesPath + "/" + "maldua_logo.png" +
      '">' +
      '</a>'

    com_btactic_twofactorauth_admin.zetaPromoWithImage =
      '<img src="' +
      com_btactic_twofactorauth_admin.zimletImagesPath + "/" + "btactic_logo.png" +
      '">' +
      " " +
      com_btactic_twofactorauth_admin.zetaPromo +
      com_btactic_twofactorauth_admin.malduaHeader;

    com_btactic_twofactorauth_admin.zetaPromoCss = "font-size:16pt; font-weight: bold;";

    // TODO: Include somehow ZsMsg.properties (and their translations) so that this is not hardcoded
    // in English
    // Apparently ZaMsg is included but ZsMsg is not
    // com_btactic_twofactorauth_admin.ZsMsg_twoFactorAuthCodeEmailSubject = ZsMsg.twoFactorAuthCodeEmailSubject
    // com_btactic_twofactorauth_admin.ZsMsg_twoFactorAuthCodeEmailBodyText = ZsMsg.twoFactorAuthCodeEmailBodyText
    // com_btactic_twofactorauth_admin.ZsMsg_twoFactorAuthCodeEmailBodyHtml = ZsMsg.twoFactorAuthCodeEmailBodyHtml

    com_btactic_twofactorauth_admin.ZsMsg_twoFactorAuthCodeEmailSubject = 'Two-factor authentication code'
    com_btactic_twofactorauth_admin.ZsMsg_twoFactorAuthCodeEmailBodyText =
      'Your two-factor authentication code is: {0}<br />' +
      'The code expires by: {1}<br />' +
      '*~*~*~*~*~*~*~*~*~*'
    com_btactic_twofactorauth_admin.ZsMsg_twoFactorAuthCodeEmailBodyHtml =
      '&lt;div style="font-family:sans-serif;"&gt;\n' +
      '&lt;hr&gt;\n' +
      'Your two-factor authentication code is: {0}&lt;br&gt;\n' +
      'The code expires by {1}\n' +
      '&lt;/div&gt;'

    com_btactic_twofactorauth_admin.emailTemplateTip =
      com_btactic_twofactorauth_admin.lbl_email_template_tip_default_values +
      '<ul>' +
      '<li><b>' + com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailFrom + '</b>' + ':' +
                  com_btactic_twofactorauth_admin.lbl_email_template_tip_from +
      '</li>' +
      '<li><b>' + com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailSubject + '</b>' + ':' +
                  com_btactic_twofactorauth_admin.ZsMsg_twoFactorAuthCodeEmailSubject +
      '</li>' +
      '<li><b>' + com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailBodyText + '</b>' + ':' +
                  com_btactic_twofactorauth_admin.ZsMsg_twoFactorAuthCodeEmailBodyText +
      '</li>' +
      '<li><b>' + com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailBodyHtml + '</b>' + ':' +
                  '<pre>' +
                      com_btactic_twofactorauth_admin.ZsMsg_twoFactorAuthCodeEmailBodyHtml +
                  '</pre>' +
      '</li>' +
      '</ul>'

    // Additional 2FA attributes - Accounts (Definition)
    if (window.ZaAccount && ZaAccount.myXModel && ZaAccount.myXModel.items) {
        ZaAccount.myXModel.items.push({id: "zimbraFeatureTwoFactorAuthAvailable", type: _COS_ENUM_, ref: "attrs/" + "zimbraFeatureTwoFactorAuthAvailable", choices: ZaModel.BOOLEAN_CHOICES});
        ZaAccount.myXModel.items.push({id: "zimbraTwoFactorAuthEnabled", type: _COS_ENUM_, ref: "attrs/" + "zimbraTwoFactorAuthEnabled", choices: ZaModel.BOOLEAN_CHOICES});
        ZaAccount.myXModel.items.push({id: "zimbraFeatureTwoFactorAuthRequired", type: _COS_ENUM_, ref: "attrs/" + "zimbraFeatureTwoFactorAuthRequired", choices: ZaModel.BOOLEAN_CHOICES});
        ZaAccount.myXModel.items.push({id: "zimbraFeatureAppSpecificPasswordsEnabled", type: _COS_ENUM_, ref: "attrs/" + "zimbraFeatureAppSpecificPasswordsEnabled", choices: ZaModel.BOOLEAN_CHOICES});
        ZaAccount.myXModel.items.push({id: "zimbraTwoFactorAuthNumScratchCodes", type: _COS_NUMBER_, ref: "attrs/" + "zimbraTwoFactorAuthNumScratchCodes", minInclusive: 1, maxInclusive: 20});
        ZaAccount.myXModel.items.push({id: "zimbraTwoFactorAuthMethodAllowed", type: _COS_LIST_, dataType: _STRING_, ref: "attrs/" + "zimbraTwoFactorAuthMethodAllowed", choices: ZaModel.ZETA_TWOFACTORAUTH_METHOD_CHOICES});
        ZaAccount.myXModel.items.push({id: "zimbraTwoFactorAuthMethodEnabled", type: _COS_LIST_, dataType: _STRING_, ref: "attrs/" + "zimbraTwoFactorAuthMethodEnabled", choices: ZaModel.ZETA_TWOFACTORAUTH_METHOD_CHOICES});
        ZaAccount.myXModel.items.push({id: "zimbraPrefPrimaryTwoFactorAuthMethod", type: _COS_ENUM_, ref: "attrs/" + "zimbraPrefPrimaryTwoFactorAuthMethod", choices: ZaModel.ZETA_TWOFACTORAUTH_METHOD_CHOICES});
        ZaAccount.myXModel.items.push({id: "zimbraTwoFactorCodeLifetimeForEmail", type: _COS_MLIFETIME_, ref: "attrs/" + "zimbraTwoFactorCodeLifetimeForEmail"});
    }

    // Additional 2FA attributes - Accounts (Edit)
    if(ZaTabView.XFormModifiers["ZaAccountXFormView"]) {
        com_btactic_twofactorauth_ext.AccountXFormModifier= function (xFormObject,entry) {

            var firstSwitchPosition = com_btactic_twofactorauth_ext.getFirstSwitchPosition(xFormObject);
            var twofactorauthTabIx = ++this.TAB_INDEX;

            var tabBar = xFormObject.items[1];
            tabBar.choices.push({value:twofactorauthTabIx, label:com_btactic_twofactorauth_admin.zimbraTwoFactorAuthTab});

            var twofactorauthAccountTab={
                type:_ZATABCASE_,
                numCols:1,
                caseKey:twofactorauthTabIx,
                items: [
                    {label: null, type: _OUTPUT_, value: com_btactic_twofactorauth_admin.zetaPromoWithImage, colSpan:"*", cssStyle:com_btactic_twofactorauth_admin.zetaPromoCss},
                    {type:_SPACER_, colSpan:"*"},
                    {type: _DWT_ALERT_, containerCssStyle: "padding-bottom:0px", style: DwtAlert.INFO, iconVisible: true, content : com_btactic_twofactorauth_admin.zimbraTwoFactorAuthDisableWarning, colSpan : "*"},
                    {type:_SPACER_, colSpan:"*"},
                    {type:_ZA_TOP_GROUPER_,
                        label:com_btactic_twofactorauth_admin.zimbraTwoFactorAuthTab,
                        items:[
                            {ref: "zimbraFeatureTwoFactorAuthAvailable", type: _SUPER_CHECKBOX_, checkBoxLabel: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthAvailable, msgName: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthAvailable, trueValue: "TRUE", falseValue: "FALSE", resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
                            {ref: "zimbraTwoFactorAuthEnabled", type: _SUPER_CHECKBOX_, checkBoxLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthEnabled, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthEnabled, trueValue: "TRUE", falseValue: "FALSE", resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
                            {ref: "zimbraFeatureTwoFactorAuthRequired", type: _SUPER_CHECKBOX_, checkBoxLabel: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthRequired, msgName: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthRequired, trueValue: "TRUE", falseValue: "FALSE", resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
                            {ref: "zimbraFeatureAppSpecificPasswordsEnabled", type: _SUPER_CHECKBOX_, checkBoxLabel: com_btactic_twofactorauth_admin.zimbraFeatureAppSpecificPasswordsEnabled, msgName: com_btactic_twofactorauth_admin.zimbraFeatureAppSpecificPasswordsEnabled, trueValue: "TRUE", falseValue: "FALSE", resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
                            {ref: "zimbraTwoFactorAuthNumScratchCodes", type: _SUPER_TEXTFIELD_, txtBoxLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthNumScratchCodes, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthNumScratchCodes, textFieldCssClass: "admin_xform_number_input", resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
                            {ref: "zimbraTwoFactorAuthMethodAllowed", type: _SUPER_MULTIPLE_CHECKBOX_, groupLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodAllowed, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodAllowed, resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
                            {ref: "zimbraTwoFactorAuthMethodEnabled", type: _ZASELECT_MULTIPLE_CHECKBOX_, groupLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodEnabled, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodEnabled, enableDisableChecks: [com_btactic_twofactorauth_ext.initDisableEnableMethods]},
                            // Inspired on ZaMsg.Check_Settings button from ZaDomainXFormView.js
                            {type:_SPACER_, colSpan:"1"}, {type: _DWT_BUTTON_, label: com_btactic_twofactorauth_admin.lbl_disable_app_2fa_method, autoPadding: false, onActivate: "com_btactic_twofactorauth_admin.disableMethod('app')", enableDisableChecks: [com_btactic_twofactorauth_ext.viewAppButton],enableDisableChangeEventSources: ["zimbraTwoFactorAuthMethodEnabled"]},
                            {type:_SPACER_, colSpan:"1"}, {type: _DWT_BUTTON_, label: com_btactic_twofactorauth_admin.lbl_disable_email_2fa_method, autoPadding: false, onActivate: "com_btactic_twofactorauth_admin.disableMethod('email')", enableDisableChecks: [com_btactic_twofactorauth_ext.viewEmailButton],enableDisableChangeEventSources: ["zimbraTwoFactorAuthMethodEnabled"]},
                            {type:_SPACER_, colSpan:"*"},
                            {ref: "zimbraPrefPrimaryTwoFactorAuthMethod", type: _OSELECT1_, label: com_btactic_twofactorauth_admin.zimbraPrefPrimaryTwoFactorAuthMethod, msgName: com_btactic_twofactorauth_admin.zimbraPrefPrimaryTwoFactorAuthMethod},
                            {ref: "zimbraTwoFactorCodeLifetimeForEmail", type: _SUPER_LIFETIME_, txtBoxLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail}
                        ]
                    }
                ]
            };

            xFormObject.items[firstSwitchPosition].items.push(twofactorauthAccountTab);
        }
        ZaTabView.XFormModifiers["ZaAccountXFormView"].push(com_btactic_twofactorauth_ext.AccountXFormModifier);
    }

    // Additional 2FA attributes - ClassOfService (Definition)
    if (window.ZaCos && ZaCos.myXModel && ZaCos.myXModel.items) {
        ZaCos.myXModel.items.push({id: "zimbraFeatureTwoFactorAuthAvailable", type: _COS_ENUM_, ref: "attrs/" + "zimbraFeatureTwoFactorAuthAvailable", choices: ZaModel.BOOLEAN_CHOICES});
        ZaCos.myXModel.items.push({id: "zimbraFeatureTwoFactorAuthRequired", type: _COS_ENUM_, ref: "attrs/" + "zimbraFeatureTwoFactorAuthRequired", choices: ZaModel.BOOLEAN_CHOICES});
        ZaCos.myXModel.items.push({id: "zimbraFeatureAppSpecificPasswordsEnabled", type: _COS_ENUM_, ref: "attrs/" + "zimbraFeatureAppSpecificPasswordsEnabled", choices: ZaModel.BOOLEAN_CHOICES});
        ZaCos.myXModel.items.push({id: "zimbraTwoFactorAuthNumScratchCodes", type: _COS_NUMBER_, ref: "attrs/" + "zimbraTwoFactorAuthNumScratchCodes", minInclusive: 1, maxInclusive: 20});
        ZaCos.myXModel.items.push({id: "zimbraTwoFactorAuthMethodAllowed", type: _COS_LIST_, dataType: _STRING_, ref: "attrs/" + "zimbraTwoFactorAuthMethodAllowed", choices: ZaModel.ZETA_TWOFACTORAUTH_METHOD_CHOICES});
        ZaCos.myXModel.items.push({id: "zimbraTwoFactorCodeLifetimeForEmail", type: _COS_MLIFETIME_, ref: "attrs/" + "zimbraTwoFactorCodeLifetimeForEmail"});
    }

    // Additional 2FA attributes - ClassOfService (Edit)
    if(ZaTabView.XFormModifiers["ZaCosXFormView"]) {
        com_btactic_twofactorauth_ext.myCosXFormModifier= function (xFormObject,entry) {

            var firstSwitchPosition = com_btactic_twofactorauth_ext.getFirstSwitchPosition(xFormObject);
            var twofactorauthTabIx = ++this.TAB_INDEX;

            var tabBar = xFormObject.items[1];
            tabBar.choices.push({value:twofactorauthTabIx, label:com_btactic_twofactorauth_admin.zimbraTwoFactorAuthTab});

            var twofactorauthAccountTab={
                type:_ZATABCASE_,
                numCols:1,
                caseKey:twofactorauthTabIx,
                items: [
                    {label: null, type: _OUTPUT_, value: com_btactic_twofactorauth_admin.zetaPromoWithImage, colSpan:"*", cssStyle:com_btactic_twofactorauth_admin.zetaPromoCss},
                    {type:_SPACER_, colSpan:"*"},
                    {type:_ZA_TOP_GROUPER_,
                        label:com_btactic_twofactorauth_admin.zimbraTwoFactorAuthTab,
                        items:[
                            {ref: "zimbraFeatureTwoFactorAuthAvailable", type: _CHECKBOX_, label: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthAvailable, msgName: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthAvailable, trueValue: "TRUE", falseValue: "FALSE", labelLocation: _LEFT_},
                            {ref: "zimbraFeatureTwoFactorAuthRequired", type: _CHECKBOX_, label: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthRequired, msgName: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthRequired, trueValue: "TRUE", falseValue: "FALSE", labelLocation: _LEFT_},
                            {ref: "zimbraFeatureAppSpecificPasswordsEnabled", type: _CHECKBOX_, label: com_btactic_twofactorauth_admin.zimbraFeatureAppSpecificPasswordsEnabled, msgName: com_btactic_twofactorauth_admin.zimbraFeatureAppSpecificPasswordsEnabled, trueValue: "TRUE", falseValue: "FALSE", labelLocation: _LEFT_},
                            {ref: "zimbraTwoFactorAuthNumScratchCodes", type: _TEXTFIELD_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthNumScratchCodes, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthNumScratchCodes, textFieldCssClass: "admin_xform_number_input", labelLocation: _LEFT_},
                            {ref: "zimbraTwoFactorAuthMethodAllowed", type: _ZASELECT_MULTIPLE_CHECKBOX_, groupLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodAllowed, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodAllowed, labelLocation: _LEFT_},
                            {ref: "zimbraTwoFactorCodeLifetimeForEmail", type: _LIFETIME_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail, labelLocation: _LEFT_}
                        ]
                    }
                ]
            };

            xFormObject.items[firstSwitchPosition].items.push(twofactorauthAccountTab);
        }
        ZaTabView.XFormModifiers["ZaCosXFormView"].push(com_btactic_twofactorauth_ext.myCosXFormModifier);
    }

    // Additional 2FA attributes - Domain (Definition)
    if (window.ZaDomain && ZaDomain.myXModel && ZaDomain.myXModel.items) {
        ZaDomain.myXModel.items.push({id: "zimbraTwoFactorAuthMethodAllowed", type: _LIST_, dataType: _STRING_, ref: "attrs/" + "zimbraTwoFactorAuthMethodAllowed", choices: ZaModel.ZETA_TWOFACTORAUTH_METHOD_CHOICES});
        ZaDomain.myXModel.items.push({id: "zimbraTwoFactorCodeLifetimeForEmail", type: _LIFETIME_, ref: "attrs/" + "zimbraTwoFactorCodeLifetimeForEmail"});
        ZaDomain.myXModel.items.push({id: "zimbraTwoFactorCodeEmailFrom", type: _EMAIL_ADDRESS_, ref: "attrs/" + "zimbraTwoFactorCodeEmailFrom"});
        ZaDomain.myXModel.items.push({id: "zimbraTwoFactorCodeEmailSubject", type: _STRING_, ref: "attrs/" + "zimbraTwoFactorCodeEmailSubject"});
        ZaDomain.myXModel.items.push({id: "zimbraTwoFactorCodeEmailBodyText", type: _STRING_, ref: "attrs/" + "zimbraTwoFactorCodeEmailBodyText"});
        ZaDomain.myXModel.items.push({id: "zimbraTwoFactorCodeEmailBodyHtml", type: _STRING_, ref: "attrs/" + "zimbraTwoFactorCodeEmailBodyHtml"});
    }

    // Additional 2FA attributes - Domain (Edit)
    if(ZaTabView.XFormModifiers["ZaDomainXFormView"]) {
        com_btactic_twofactorauth_ext.myDomainXFormModifier= function (xFormObject,entry) {

            var firstSwitchPosition = com_btactic_twofactorauth_ext.getFirstSwitchPosition(xFormObject);
            var twofactorauthTabIx = ++this.TAB_INDEX;

            var tabBar = xFormObject.items[1];
            tabBar.choices.push({value:twofactorauthTabIx, label:com_btactic_twofactorauth_admin.zimbraTwoFactorAuthTab});

            var twofactorauthAccountTab={
                type:_ZATABCASE_,
                numCols:1,
                caseKey:twofactorauthTabIx,
                items: [
                    {label: null, type: _OUTPUT_, value: com_btactic_twofactorauth_admin.zetaPromoWithImage, colSpan:"*", cssStyle:com_btactic_twofactorauth_admin.zetaPromoCss},
                    {type:_SPACER_, colSpan:"*"},
                    {type: _DWT_ALERT_, containerCssStyle: "padding-bottom:0px", style: DwtAlert.INFO, iconVisible: true, content : com_btactic_twofactorauth_admin.emailTemplateTip, colSpan : "*"},
                    {type:_SPACER_, colSpan:"*"},
                    {type:_ZA_TOP_GROUPER_,
                        label:com_btactic_twofactorauth_admin.zimbraTwoFactorAuthTab,
                        items:[
                            {ref: "zimbraTwoFactorAuthMethodAllowed", type: _ZASELECT_MULTIPLE_CHECKBOX_, groupLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodAllowed, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodAllowed, labelLocation: _LEFT_},
                            {ref: "zimbraTwoFactorCodeLifetimeForEmail", type: _LIFETIME_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail, labelLocation: _LEFT_},
                            {ref: "zimbraTwoFactorCodeEmailFrom", type: _TEXTFIELD_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailFrom, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailFrom, labelLocation: _LEFT_, enableDisableChecks: [com_btactic_twofactorauth_ext.viewEmailFrom]},
                            {ref: "zimbraTwoFactorCodeEmailSubject", type: _TEXTFIELD_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailSubject, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailSubject, labelLocation: _LEFT_},
                            {ref: "zimbraTwoFactorCodeEmailBodyText", type: _TEXTAREA_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailBodyText, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailBodyText, labelLocation: _LEFT_},
                            {ref: "zimbraTwoFactorCodeEmailBodyHtml", type: _TEXTAREA_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailBodyHtml, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailBodyHtml, labelLocation: _LEFT_}
                        ]
                    }
                ]
            };

            xFormObject.items[firstSwitchPosition].items.push(twofactorauthAccountTab);
        }
        ZaTabView.XFormModifiers["ZaDomainXFormView"].push(com_btactic_twofactorauth_ext.myDomainXFormModifier);
    }

    // Additional 2FA attributes - GlobalConfig (Definition)
    if (window.ZaGlobalConfig && ZaGlobalConfig.myXModel && ZaGlobalConfig.myXModel.items) {
        ZaGlobalConfig.myXModel.items.push({id: "zimbraTwoFactorCodeLifetimeForEmail", type: _COS_MLIFETIME_, ref: "attrs/" + "zimbraTwoFactorCodeLifetimeForEmail"});
        ZaGlobalConfig.myXModel.items.push({id: "zimbraTwoFactorCodeEmailFrom", type: _EMAIL_ADDRESS_, ref: "attrs/" + "zimbraTwoFactorCodeEmailFrom"});
        ZaGlobalConfig.myXModel.items.push({id: "zimbraTwoFactorCodeEmailSubject", type: _STRING_, ref: "attrs/" + "zimbraTwoFactorCodeEmailSubject"});
        ZaGlobalConfig.myXModel.items.push({id: "zimbraTwoFactorCodeEmailBodyText", type: _STRING_, ref: "attrs/" + "zimbraTwoFactorCodeEmailBodyText"});
        ZaGlobalConfig.myXModel.items.push({id: "zimbraTwoFactorCodeEmailBodyHtml", type: _STRING_, ref: "attrs/" + "zimbraTwoFactorCodeEmailBodyHtml"});
    }

    // Additional 2FA attributes - GlobalConfig (Edit)
    if(ZaTabView.XFormModifiers["GlobalConfigXFormView"]) {
        com_btactic_twofactorauth_ext.myGlobalConfigXFormModifier= function (xFormObject,entry) {

            var firstSwitchPosition = com_btactic_twofactorauth_ext.getFirstSwitchPosition(xFormObject);
            var twofactorauthTabIx = ++this.TAB_INDEX;

            var tabBar = xFormObject.items[1];
            tabBar.choices.push({value:twofactorauthTabIx, label:com_btactic_twofactorauth_admin.zimbraTwoFactorAuthTab});

            var twofactorauthAccountTab={
                type:_ZATABCASE_,
                numCols:1,
                caseKey:twofactorauthTabIx,
                items: [
                    {label: null, type: _OUTPUT_, value: com_btactic_twofactorauth_admin.zetaPromoWithImage, colSpan:"*", cssStyle:com_btactic_twofactorauth_admin.zetaPromoCss},
                    {type:_SPACER_, colSpan:"*"},
                    {type: _DWT_ALERT_, containerCssStyle: "padding-bottom:0px", style: DwtAlert.INFO, iconVisible: true, content : com_btactic_twofactorauth_admin.emailTemplateTip, colSpan : "*"},
                    {type:_SPACER_, colSpan:"*"},
                    {type:_ZA_TOP_GROUPER_,
                        label:com_btactic_twofactorauth_admin.zimbraTwoFactorAuthTab,
                        items:[
                            {ref: "zimbraTwoFactorCodeLifetimeForEmail", type: _LIFETIME_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail, labelLocation: _LEFT_},
                            {ref: "zimbraTwoFactorCodeEmailFrom", type: _TEXTFIELD_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailFrom, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailFrom, labelLocation: _LEFT_, enableDisableChecks: [com_btactic_twofactorauth_ext.viewEmailFrom]},
                            {ref: "zimbraTwoFactorCodeEmailSubject", type: _TEXTFIELD_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailSubject, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailSubject, labelLocation: _LEFT_},
                            {ref: "zimbraTwoFactorCodeEmailBodyText", type: _TEXTAREA_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailBodyText, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailBodyText, labelLocation: _LEFT_},
                            {ref: "zimbraTwoFactorCodeEmailBodyHtml", type: _TEXTAREA_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailBodyHtml, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailBodyHtml, labelLocation: _LEFT_}
                        ]
                    }
                ]
            };

            xFormObject.items[firstSwitchPosition].items.push(twofactorauthAccountTab);
        }
        ZaTabView.XFormModifiers["GlobalConfigXFormView"].push(com_btactic_twofactorauth_ext.myGlobalConfigXFormModifier);
    }

    // Additional 2FA attributes - Accounts (New)
    com_btactic_twofactorauth_ext.ACC_WIZ_GROUP = {
        type:_ZAWIZGROUP_,
        items:[
            {label: null, type: _OUTPUT_, value: com_btactic_twofactorauth_admin.zetaPromoWithImage, colSpan:"*", cssStyle:com_btactic_twofactorauth_admin.zetaPromoCss},
            {type:_SPACER_, colSpan:"*"},
            {
              type: _ZAWIZ_TOP_GROUPER_,
              label:com_btactic_twofactorauth_admin.zimbraTwoFactorAuthTab,
              colSizes : [ "200px", "400px" ],
              numCols : 2,
              items : [
                {ref: "zimbraFeatureTwoFactorAuthAvailable", type: _SUPER_WIZ_CHECKBOX_, checkBoxLabel: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthAvailable, msgName: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthAvailable, trueValue: "TRUE", falseValue: "FALSE", resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
                {ref: "zimbraFeatureTwoFactorAuthRequired", type: _SUPER_WIZ_CHECKBOX_, checkBoxLabel: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthRequired, msgName: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthRequired, trueValue: "TRUE", falseValue: "FALSE", resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
                {ref: "zimbraFeatureAppSpecificPasswordsEnabled", type: _SUPER_WIZ_CHECKBOX_, checkBoxLabel: com_btactic_twofactorauth_admin.zimbraFeatureAppSpecificPasswordsEnabled, msgName: com_btactic_twofactorauth_admin.zimbraFeatureAppSpecificPasswordsEnabled, trueValue: "TRUE", falseValue: "FALSE", resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
                {ref: "zimbraTwoFactorAuthNumScratchCodes", type: _SUPERWIZ_TEXTFIELD_, txtBoxLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthNumScratchCodes, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthNumScratchCodes, textFieldCssClass: "admin_xform_number_input", resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
                {ref: "zimbraTwoFactorAuthMethodAllowed", type: _SUPER_WIZ_MULTIPLE_CHECKBOX_, groupLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodAllowed, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodAllowed, resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
                {ref: "zimbraTwoFactorCodeLifetimeForEmail", type: _SUPERWIZ_LIFETIME_, txtBoxLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail, resetToSuperLabel: ZaMsg.NAD_ResetToCOS}
              ]
            }
        ]
    };

    if(ZaXDialog.XFormModifiers["ZaNewAccountXWizard"]) {
        com_btactic_twofactorauth_ext.AccountXWizModifier= function (xFormObject, entry) {

            var firstSwitchPosition = com_btactic_twofactorauth_ext.getFirstSwitchPosition(xFormObject);
            var twofactorauthTabIx = ++this.TAB_INDEX;

            this.stepChoices.push({value:twofactorauthTabIx, label:com_btactic_twofactorauth_admin.zimbraTwoFactorAuthTab});
            this._lastStep = this.stepChoices.length;

            var twofactorauthStep={type:_CASE_, numCols:1, caseKey:twofactorauthTabIx, tabGroupKey:twofactorauthTabIx,
                items: [com_btactic_twofactorauth_ext.ACC_WIZ_GROUP]
            };

            xFormObject.items[firstSwitchPosition].items.push(twofactorauthStep);

        }
        ZaXDialog.XFormModifiers["ZaNewAccountXWizard"].push(com_btactic_twofactorauth_ext.AccountXWizModifier);
    }

    // Additional 2FA attributes - ClassOfService (New)
    com_btactic_twofactorauth_ext.COS_WIZ_GROUP = {
        type:_ZAWIZGROUP_,
        items:[
            {label: null, type: _OUTPUT_, value: com_btactic_twofactorauth_admin.zetaPromoWithImage, colSpan:"*", cssStyle:com_btactic_twofactorauth_admin.zetaPromoCss},
            {type:_SPACER_, colSpan:"*"},
            {
              type: _ZAWIZ_TOP_GROUPER_,
              label:com_btactic_twofactorauth_admin.zimbraTwoFactorAuthTab,
              colSizes : [ "200px", "400px" ],
              numCols : 2,
              items : [
                {ref: "zimbraFeatureTwoFactorAuthAvailable", type: _WIZ_CHECKBOX_, label: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthAvailable, msgName: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthAvailable, trueValue: "TRUE", falseValue: "FALSE"},
                {ref: "zimbraFeatureTwoFactorAuthRequired", type: _WIZ_CHECKBOX_, label: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthRequired, msgName: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthRequired, trueValue: "TRUE", falseValue: "FALSE"},
                {ref: "zimbraFeatureAppSpecificPasswordsEnabled", type: _WIZ_CHECKBOX_, label: com_btactic_twofactorauth_admin.zimbraFeatureAppSpecificPasswordsEnabled, msgName: com_btactic_twofactorauth_admin.zimbraFeatureAppSpecificPasswordsEnabled, trueValue: "TRUE", falseValue: "FALSE"},
                {ref: "zimbraTwoFactorAuthNumScratchCodes", type: _TEXTFIELD_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthNumScratchCodes, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthNumScratchCodes, textFieldCssClass: "admin_xform_number_input"},
                {ref: "zimbraTwoFactorAuthMethodAllowed", type: _ZASELECT_WIZ_MULTIPLE_CHECKBOX_, groupLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodAllowed, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodAllowed},
                {ref: "zimbraTwoFactorCodeLifetimeForEmail", type: _LIFETIME_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail}
              ]
            }
        ]
    };

    if(ZaXDialog.XFormModifiers["ZaNewCosXWizard"]) {
        com_btactic_twofactorauth_ext.CosXWizModifier= function (xFormObject, entry) {

            var firstSwitchPosition = com_btactic_twofactorauth_ext.getFirstSwitchPosition(xFormObject);
            var twofactorauthTabIx = ++this.TAB_INDEX;

            this.stepChoices.push({value:twofactorauthTabIx, label:com_btactic_twofactorauth_admin.zimbraTwoFactorAuthTab});
            this._lastStep = this.stepChoices.length;

            var twofactorauthStep={type:_CASE_, numCols:1, caseKey:twofactorauthTabIx, tabGroupKey:twofactorauthTabIx,
                items: [com_btactic_twofactorauth_ext.COS_WIZ_GROUP]
            };

            xFormObject.items[firstSwitchPosition].items.push(twofactorauthStep);

        }
        ZaXDialog.XFormModifiers["ZaNewCosXWizard"].push(com_btactic_twofactorauth_ext.CosXWizModifier);
    }

    // Additional 2FA attributes - Domain (New)
    com_btactic_twofactorauth_ext.DOMAIN_WIZ_GROUP = {
        type:_ZAWIZGROUP_,
        items:[
            {label: null, type: _OUTPUT_, value: com_btactic_twofactorauth_admin.zetaPromoWithImage, colSpan:"*", cssStyle:com_btactic_twofactorauth_admin.zetaPromoCss},
            {type:_SPACER_, colSpan:"*"},
            {type: _DWT_ALERT_, containerCssStyle: "padding-bottom:0px", style: DwtAlert.INFO, iconVisible: true, content : com_btactic_twofactorauth_admin.emailTemplateTip, colSpan : "*"},
            {type:_SPACER_, colSpan:"*"},
            {
              type: _ZAWIZ_TOP_GROUPER_,
              label:com_btactic_twofactorauth_admin.zimbraTwoFactorAuthTab,
              colSizes : [ "200px", "400px" ],
              numCols : 2,
              items : [
                {ref: "zimbraTwoFactorAuthMethodAllowed", type: _ZASELECT_WIZ_MULTIPLE_CHECKBOX_, groupLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodAllowed, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodAllowed, labelLocation: _LEFT_},
                {ref: "zimbraTwoFactorCodeLifetimeForEmail", type: _LIFETIME_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail},
                {ref: "zimbraTwoFactorCodeEmailFrom", type: _TEXTFIELD_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailFrom, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailFrom, enableDisableChecks: [com_btactic_twofactorauth_ext.viewEmailFrom]},
                {ref: "zimbraTwoFactorCodeEmailSubject", type: _TEXTFIELD_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailSubject, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailSubject},
                {ref: "zimbraTwoFactorCodeEmailBodyText", type: _TEXTAREA_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailBodyText, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailBodyText},
                {ref: "zimbraTwoFactorCodeEmailBodyHtml", type: _TEXTAREA_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailBodyHtml, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeEmailBodyHtml}
              ]
            }
        ]
    };

    if(ZaXDialog.XFormModifiers["ZaNewDomainXWizard"]) {
        com_btactic_twofactorauth_ext.DomainXWizModifier= function (xFormObject, entry) {

            var firstSwitchPosition = com_btactic_twofactorauth_ext.getFirstSwitchPosition(xFormObject);
            var twofactorauthTabIx = this.TAB_INDEX; // We do not want latest position (++this.TAB_INDEX) but almost latest position
            ZaNewDomainXWizard.CONFIG_COMPLETE_STEP = twofactorauthTabIx + 1 ; // Ensure that Complete step is the last one.

            var endStep = this.stepChoices.pop();
            endStep.value = endStep.value + 1 // Move the endStep downwards

            this.stepChoices.push({value:twofactorauthTabIx, label:com_btactic_twofactorauth_admin.zimbraTwoFactorAuthTab});
            this.stepChoices.push(endStep);
            this._lastStep = this.stepChoices.length;

            var twofactorauthStep={type:_CASE_, numCols:1, caseKey:twofactorauthTabIx,
                items: [com_btactic_twofactorauth_ext.DOMAIN_WIZ_GROUP]
            };

            var switchListEnd = xFormObject.items[firstSwitchPosition].items.pop();
            switchListEnd.caseKey = switchListEnd.caseKey + 1 // Move the switchListEnd downwards

            xFormObject.items[firstSwitchPosition].items.push(twofactorauthStep);
            xFormObject.items[firstSwitchPosition].items.push(switchListEnd);

        }
        ZaXDialog.XFormModifiers["ZaNewDomainXWizard"].push(com_btactic_twofactorauth_ext.DomainXWizModifier);
    }

}
