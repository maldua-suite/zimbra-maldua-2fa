/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra OSE 2FA Extension
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

    // Inspired from ZaModel.getBooleanChoices
    // TODO: Use translations instead of hardcoded strings
    ZaModel.getZetaTwoFactorAuthMethodChoices = function () {
        return [
            {
                value: "app",
                label: "App"
            },
            {
                value: "email",
                label: "Email"
            }
        ];
    }

    ZaModel.ZETA_TWOFACTORAUTH_METHOD_CHOICES= ZaModel.getZetaTwoFactorAuthMethodChoices ;

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
            var cnt = xFormObject.items.length;
            var i = 0;
            for(i = 0; i <cnt; i++) {
                if(xFormObject.items[i].type=="switch")
                    break;
            }
            var tabBar = xFormObject.items[1] ;
            var twofactorauthTabIx = ++this.TAB_INDEX;
            tabBar.choices.push({value:twofactorauthTabIx, label:com_btactic_twofactorauth_admin.zimbraTwoFactorAuthTab});

            var twofactorauthAccountTab={
                type:_ZATABCASE_,
                numCols:1,
                caseKey:twofactorauthTabIx,
                items: [
                    {label: null, type: _OUTPUT_, value: com_btactic_twofactorauth_admin.zetaPromo, colSpan:"*", cssStyle:"font-size:20pt; font-weight: bold;"},
                    {type:_SPACER_, colSpan:"*"},
                    {type: _DWT_ALERT_, containerCssStyle: "padding-bottom:0px", style: DwtAlert.INFO, iconVisible: true, content : com_btactic_twofactorauth_admin.zimbraTwoFactorAuthDisableWarning, colSpan : "*"},
                    {type:_SPACER_, colSpan:"*"},
                    {type:_ZAGROUP_,
                        items:[
                            {ref: "zimbraFeatureTwoFactorAuthAvailable", type: _SUPER_CHECKBOX_, checkBoxLabel: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthAvailable, msgName: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthAvailable, trueValue: "TRUE", falseValue: "FALSE", resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
                            {ref: "zimbraTwoFactorAuthEnabled", type: _SUPER_CHECKBOX_, checkBoxLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthEnabled, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthEnabled, trueValue: "TRUE", falseValue: "FALSE", resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
                            {ref: "zimbraFeatureTwoFactorAuthRequired", type: _SUPER_CHECKBOX_, checkBoxLabel: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthRequired, msgName: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthRequired, trueValue: "TRUE", falseValue: "FALSE", resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
                            {ref: "zimbraFeatureAppSpecificPasswordsEnabled", type: _SUPER_CHECKBOX_, checkBoxLabel: com_btactic_twofactorauth_admin.zimbraFeatureAppSpecificPasswordsEnabled, msgName: com_btactic_twofactorauth_admin.zimbraFeatureAppSpecificPasswordsEnabled, trueValue: "TRUE", falseValue: "FALSE", resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
                            {ref: "zimbraTwoFactorAuthNumScratchCodes", type: _SUPER_TEXTFIELD_, txtBoxLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthNumScratchCodes, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthNumScratchCodes, textFieldCssClass: "admin_xform_number_input", resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
                            {ref: "zimbraTwoFactorAuthMethodAllowed", type: _SUPER_MULTIPLE_CHECKBOX_, groupLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodAllowed, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodAllowed, resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
                            {ref: "zimbraTwoFactorAuthMethodEnabled", type: _ZASELECT_MULTIPLE_CHECKBOX_, groupLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodEnabled, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodEnabled},
                            {ref: "zimbraPrefPrimaryTwoFactorAuthMethod", type: _OSELECT1_, label: com_btactic_twofactorauth_admin.zimbraPrefPrimaryTwoFactorAuthMethod, msgName: com_btactic_twofactorauth_admin.zimbraPrefPrimaryTwoFactorAuthMethod},
                            {ref: "zimbraTwoFactorCodeLifetimeForEmail", type: _SUPER_LIFETIME_, txtBoxLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail}
                        ]
                    }
                ]
            };

            xFormObject.items[i].items.push(twofactorauthAccountTab);
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
            var cnt = xFormObject.items.length;
            var i = 0;
            for(i = 0; i <cnt; i++) {
                if(xFormObject.items[i].type=="switch")
                    break;
            }
            var tabBar = xFormObject.items[1] ;
            var twofactorauthTabIx = ++this.TAB_INDEX;
            tabBar.choices.push({value:twofactorauthTabIx, label:com_btactic_twofactorauth_admin.zimbraTwoFactorAuthTab});

            var twofactorauthAccountTab={
                type:_ZATABCASE_,
                numCols:1,
                caseKey:twofactorauthTabIx,
                items: [
                    {label: null, type: _OUTPUT_, value: com_btactic_twofactorauth_admin.zetaPromo, colSpan:"*", cssStyle:"font-size:20pt; font-weight: bold;"},
                    {type:_SPACER_, colSpan:"*"},
                    {type:_ZAGROUP_,
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

            xFormObject.items[i].items.push(twofactorauthAccountTab);
        }
        ZaTabView.XFormModifiers["ZaCosXFormView"].push(com_btactic_twofactorauth_ext.myCosXFormModifier);
    }

    // Additional 2FA attributes - Domain (Definition)
    if (window.ZaDomain && ZaDomain.myXModel && ZaDomain.myXModel.items) {
        ZaDomain.myXModel.items.push({id: "zimbraTwoFactorCodeLifetimeForEmail", type: _COS_MLIFETIME_, ref: "attrs/" + "zimbraTwoFactorCodeLifetimeForEmail"});
    }

    // Additional 2FA attributes - Domain (Edit)
    if(ZaTabView.XFormModifiers["ZaDomainXFormView"]) {
        com_btactic_twofactorauth_ext.myDomainXFormModifier= function (xFormObject,entry) {
            var cnt = xFormObject.items.length;
            var i = 0;
            for(i = 0; i <cnt; i++) {
                if(xFormObject.items[i].type=="switch")
                    break;
            }
            var tabBar = xFormObject.items[1] ;
            var twofactorauthTabIx = ++this.TAB_INDEX;
            tabBar.choices.push({value:twofactorauthTabIx, label:com_btactic_twofactorauth_admin.zimbraTwoFactorAuthTab});

            var twofactorauthAccountTab={
                type:_ZATABCASE_,
                numCols:1,
                caseKey:twofactorauthTabIx,
                items: [
                    {label: null, type: _OUTPUT_, value: com_btactic_twofactorauth_admin.zetaPromo, colSpan:"*", cssStyle:"font-size:20pt; font-weight: bold;"},
                    {type:_SPACER_, colSpan:"*"},
                    {type:_ZAGROUP_,
                        items:[
                            {ref: "zimbraTwoFactorCodeLifetimeForEmail", type: _LIFETIME_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail, labelLocation: _LEFT_}
                        ]
                    }
                ]
            };

            xFormObject.items[i].items.push(twofactorauthAccountTab);
        }
        ZaTabView.XFormModifiers["ZaDomainXFormView"].push(com_btactic_twofactorauth_ext.myDomainXFormModifier);
    }

    // Additional 2FA attributes - Accounts (New)
    com_btactic_twofactorauth_ext.ACC_WIZ_GROUP = {
        type:_ZAWIZGROUP_,
        items:[
            {label: null, type: _OUTPUT_, value: com_btactic_twofactorauth_admin.zetaPromo, colSpan:"*", cssStyle:"font-size:20pt; font-weight: bold;"},
            {type:_SPACER_, colSpan:"*"},
            {ref: "zimbraFeatureTwoFactorAuthAvailable", type: _SUPER_WIZ_CHECKBOX_, checkBoxLabel: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthAvailable, msgName: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthAvailable, trueValue: "TRUE", falseValue: "FALSE", resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
            {ref: "zimbraFeatureTwoFactorAuthRequired", type: _SUPER_WIZ_CHECKBOX_, checkBoxLabel: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthRequired, msgName: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthRequired, trueValue: "TRUE", falseValue: "FALSE", resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
            {ref: "zimbraFeatureAppSpecificPasswordsEnabled", type: _SUPER_WIZ_CHECKBOX_, checkBoxLabel: com_btactic_twofactorauth_admin.zimbraFeatureAppSpecificPasswordsEnabled, msgName: com_btactic_twofactorauth_admin.zimbraFeatureAppSpecificPasswordsEnabled, trueValue: "TRUE", falseValue: "FALSE", resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
            {ref: "zimbraTwoFactorAuthNumScratchCodes", type: _SUPERWIZ_TEXTFIELD_, txtBoxLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthNumScratchCodes, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthNumScratchCodes, textFieldCssClass: "admin_xform_number_input", resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
            {ref: "zimbraTwoFactorAuthMethodAllowed", type: _SUPER_WIZ_MULTIPLE_CHECKBOX_, groupLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodAllowed, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodAllowed, resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
            {ref: "zimbraTwoFactorCodeLifetimeForEmail", type: _SUPERWIZ_LIFETIME_, txtBoxLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail, resetToSuperLabel: ZaMsg.NAD_ResetToCOS},
        ]
    };

    if(ZaXDialog.XFormModifiers["ZaNewAccountXWizard"]) {
        com_btactic_twofactorauth_ext.AccountXWizModifier= function (xFormObject, entry) {
            ZaNewAccountXWizard.POSIX_2FA_STEP = ++this.TAB_INDEX;
            this.stepChoices.push({value:ZaNewAccountXWizard.POSIX_2FA_STEP, label:com_btactic_twofactorauth_admin.zimbraTwoFactorAuthTab});
            this._lastStep = this.stepChoices.length;

            var cnt = xFormObject.items.length;
            var i = 0;
            for(i = 0; i <cnt; i++) {
                if(xFormObject.items[i].type=="switch")
                    break;
            }
            cnt = xFormObject.items[i].items.length;
            var j = 0;
            var gotAdvanced = false;
            var gotFeatures = false;
            var twofactorauthStep={type:_CASE_, numCols:1, caseKey:ZaNewAccountXWizard.POSIX_2FA_STEP, tabGroupKey:ZaNewAccountXWizard.POSIX_2FA_STEP,
                items: [com_btactic_twofactorauth_ext.ACC_WIZ_GROUP]
            };
            xFormObject.items[i].items.push(twofactorauthStep);

        }
        ZaXDialog.XFormModifiers["ZaNewAccountXWizard"].push(com_btactic_twofactorauth_ext.AccountXWizModifier);
    }

    // Additional 2FA attributes - ClassOfService (New)
    com_btactic_twofactorauth_ext.COS_WIZ_GROUP = {
        type:_ZAWIZGROUP_,
        items:[
            {label: null, type: _OUTPUT_, value: com_btactic_twofactorauth_admin.zetaPromo, colSpan:"*", cssStyle:"font-size:20pt; font-weight: bold;"},
            {type:_SPACER_, colSpan:"*"},
            {ref: "zimbraFeatureTwoFactorAuthAvailable", type: _WIZ_CHECKBOX_, label: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthAvailable, msgName: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthAvailable, trueValue: "TRUE", falseValue: "FALSE"},
            {ref: "zimbraFeatureTwoFactorAuthRequired", type: _WIZ_CHECKBOX_, label: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthRequired, msgName: com_btactic_twofactorauth_admin.zimbraFeatureTwoFactorAuthRequired, trueValue: "TRUE", falseValue: "FALSE"},
            {ref: "zimbraFeatureAppSpecificPasswordsEnabled", type: _WIZ_CHECKBOX_, label: com_btactic_twofactorauth_admin.zimbraFeatureAppSpecificPasswordsEnabled, msgName: com_btactic_twofactorauth_admin.zimbraFeatureAppSpecificPasswordsEnabled, trueValue: "TRUE", falseValue: "FALSE"},
            {ref: "zimbraTwoFactorAuthNumScratchCodes", type: _TEXTFIELD_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthNumScratchCodes, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthNumScratchCodes, textFieldCssClass: "admin_xform_number_input"},
            {ref: "zimbraTwoFactorAuthMethodAllowed", type: _ZASELECT_WIZ_MULTIPLE_CHECKBOX_, groupLabel: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodAllowed, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorAuthMethodAllowed},
            {ref: "zimbraTwoFactorCodeLifetimeForEmail", type: _LIFETIME_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail}
        ]
    };

    if(ZaXDialog.XFormModifiers["ZaNewCosXWizard"]) {
        com_btactic_twofactorauth_ext.CosXWizModifier= function (xFormObject, entry) {
            ZaNewCosXWizard.POSIX_2FA_STEP = ++this.TAB_INDEX;
            this.stepChoices.push({value:ZaNewCosXWizard.POSIX_2FA_STEP, label:com_btactic_twofactorauth_admin.zimbraTwoFactorAuthTab});
            this._lastStep = this.stepChoices.length;

            var cnt = xFormObject.items.length;
            var i = 0;
            for(i = 0; i <cnt; i++) {
                if(xFormObject.items[i].type=="switch")
                    break;
            }
            cnt = xFormObject.items[i].items.length;
            var j = 0;
            var gotAdvanced = false;
            var gotFeatures = false;
            var twofactorauthStep={type:_CASE_, numCols:1, caseKey:ZaNewCosXWizard.POSIX_2FA_STEP, tabGroupKey:ZaNewCosXWizard.POSIX_2FA_STEP,
                items: [com_btactic_twofactorauth_ext.COS_WIZ_GROUP]
            };
            xFormObject.items[i].items.push(twofactorauthStep);

        }
        ZaXDialog.XFormModifiers["ZaNewCosXWizard"].push(com_btactic_twofactorauth_ext.CosXWizModifier);
    }

    // Additional 2FA attributes - Domain (New)
    com_btactic_twofactorauth_ext.DOMAIN_WIZ_GROUP = {
        type:_ZAWIZGROUP_,
        items:[
            {label: null, type: _OUTPUT_, value: com_btactic_twofactorauth_admin.zetaPromo, colSpan:"*", cssStyle:"font-size:20pt; font-weight: bold;"},
            {type:_SPACER_, colSpan:"*"},
            {ref: "zimbraTwoFactorCodeLifetimeForEmail", type: _LIFETIME_, label: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail, msgName: com_btactic_twofactorauth_admin.zimbraTwoFactorCodeLifetimeForEmail}
        ]
    };

    if(ZaXDialog.XFormModifiers["ZaNewDomainXWizard"]) {
        com_btactic_twofactorauth_ext.DomainXWizModifier= function (xFormObject, entry) {

            ZaNewDomainXWizard.POSIX_2FA_STEP = this.TAB_INDEX; // We do not want latest position (++this.TAB_INDEX) but almost latest position
            ZaNewDomainXWizard.CONFIG_COMPLETE_STEP = ZaNewDomainXWizard.POSIX_2FA_STEP + 1 ; // Ensure that Complete step is the last one.

            var endStep = this.stepChoices.pop();
            endStep.value = endStep.value + 1 // Move the endStep downwards

            console.log("DEBUG1 - this.stepChoices - BEGIN");
            console.log(this.stepChoices);
            console.log("DEBUG1 - this.stepChoices - END");

            this.stepChoices.push({value:ZaNewDomainXWizard.POSIX_2FA_STEP, label:com_btactic_twofactorauth_admin.zimbraTwoFactorAuthTab});
            this.stepChoices.push(endStep);

            this._lastStep = this.stepChoices.length;

            var cnt = xFormObject.items.length;
            var i = 0;
            for(i = 0; i <cnt; i++) {
                if(xFormObject.items[i].type=="switch")
                    break;
            }
            cnt = xFormObject.items[i].items.length;
            var j = 0;
            var gotAdvanced = false;
            var gotFeatures = false;
            var twofactorauthStep={type:_CASE_, numCols:1, caseKey:ZaNewDomainXWizard.POSIX_2FA_STEP,
                items: [com_btactic_twofactorauth_ext.DOMAIN_WIZ_GROUP]
            };

            var switchListEnd = xFormObject.items[i].items.pop();
            switchListEnd.caseKey = switchListEnd.caseKey + 1 // Move the switchListEnd downwards

            xFormObject.items[i].items.push(twofactorauthStep);
            xFormObject.items[i].items.push(switchListEnd);

            console.log("DEBUG1 - xFormObject - BEGIN");
            console.log(xFormObject);
            console.log("DEBUG1 - xFormObject - END");

            console.log("DEBUG1 - entry - BEGIN");
            console.log(entry);
            console.log("DEBUG1 - entry - END");

        }
        ZaXDialog.XFormModifiers["ZaNewDomainXWizard"].push(com_btactic_twofactorauth_ext.DomainXWizModifier);
    }

}
