/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra OSE 2FA Extension
 * Copyright (C) 2023 BTACTIC, S.C.C.L.
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.btactic.twofactorauth.service;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.account.AccountDocumentHandler;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.soap.account.message.SendTwoFactorAuthCodeRequest;
import com.zimbra.soap.account.message.SendTwoFactorAuthCodeRequest.SendTwoFactorAuthCodeAction;
import com.zimbra.soap.account.message.SendTwoFactorAuthCodeResponse;
import com.zimbra.soap.account.message.SendTwoFactorAuthCodeResponse.SendTwoFactorAuthCodeStatus;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;

import com.btactic.twofactorauth.ZetaTwoFactorAuth;

import com.zimbra.common.util.ZimbraLog;

/** SOAP handler to enable two-factor auth.
 * @author iraykin
 *
 */
public class SendEmailMethod extends TwoFactorAuthMethod {

    @Override
    protected SendTwoFactorAuthCodeStatus doMethod(Element request, Map<String, Object> context)
            throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        ZimbraSoapContext zsc = AccountDocumentHandler.getZimbraSoapContext(context);
        SendTwoFactorAuthCodeRequest req = JaxbUtil.elementToJaxb(request);

        AuthToken at;
        Account authTokenAcct;

        // TODO: Should we get the AuthToken from the SendTwoFactorAuthCodeRequest
        // instead of zcs
        // because the token is sent at the same level of action in `SendTwoFactorAuthCodeTag.java` file
        // ?
        at = zsc.getAuthToken();
        authTokenAcct = AuthProvider.validateAuthToken(prov, at, false, Usage.TWO_FACTOR_AUTH);

        String recoveryEmail = authTokenAcct.getPrefPasswordRecoveryAddress();
        boolean emailIsSent = false;
        if (recoveryEmail != null) {
          try {
            ZetaTwoFactorAuth manager = new ZetaTwoFactorAuth(authTokenAcct);
            String code = manager.storeEmailCode();
            Mailbox mbox = getRequestedMailbox(zsc);
            OperationContext octxt = getOperationContext(zsc, context);
            sendEmail(authTokenAcct, recoveryEmail, mbox, code, zsc, octxt);
            emailIsSent = true;
          } catch (ServiceException e) {
            emailIsSent = false;
          }
        } else {
          throw ServiceException.FAILURE("Non supported wizard input.", null);
        }

        // TODO: Add logic for when sending email cannot be done properly.
        if (emailIsSent) {
          return SendTwoFactorAuthCodeStatus.SENT;
        } else {
          return SendTwoFactorAuthCodeStatus.NOT_SENT;
        }
    }

    public void sendEmail(Account account, String toEmail, Mailbox mbox, String code,
            ZimbraSoapContext zsc, OperationContext octxt) throws ServiceException {
        // Inspired from sendAndStoreTwoFactorAuthAccountCode function from EmailChannel.java file
        Locale locale = account.getLocale();
        String ownerAcctDisplayName = account.getDisplayName();
        if (ownerAcctDisplayName == null) {
            ownerAcctDisplayName = account.getName();
        }
        String subject = L10nUtil.getMessage(MsgKey.twoFactorAuthEmailSubject, locale, ownerAcctDisplayName);
        String charset = account.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, MimeConstants.P_CHARSET_UTF8);
        try {
            DateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
            format.setTimeZone(TimeZone.getTimeZone(Util.getAccountTimeZone(account).getID()));
            String dateTime = format.format(Long.valueOf(recoveryCodeMap.get(CodeConstants.EXPIRY_TIME.toString())));
            if (ZimbraLog.passwordreset.isDebugEnabled()) {
                ZimbraLog.passwordreset.debug(
                        "sendTwoFactorAuthEmailVerificationCode: Expiry of two-factor auth email address verification code sent to %s: %s",
                        toEmail, dateTime);
                ZimbraLog.passwordreset.debug(
                        "sendTwoFactorAuthEmailVerificationCode: Last 3 characters of two-factor auth email verification code sent to %s: %s",
                        toEmail,
                        code);
            }
            String mimePartText = L10nUtil.getMessage(MsgKey.twoFactorAuthEmailBodyText, locale,
                    recoveryCodeMap.get(CodeConstants.CODE.toString()), dateTime);
            String mimePartHtml = L10nUtil.getMessage(MsgKey.twoFactorAuthEmailBodyHtml, locale,
                    recoveryCodeMap.get(CodeConstants.CODE.toString()), dateTime);
            MimeMultipart mmp = AccountUtil.generateMimeMultipart(mimePartText, mimePartHtml, null);
            MimeMessage mm = AccountUtil.generateMimeMessage(account, account, subject, charset, null, null,
                    toEmail, mmp);
            mbox.getMailSender().sendMimeMessage(octxt, mbox, false, mm, null, null, null, null, false);
        } catch (MessagingException e) {
            ZimbraLog.passwordreset.warn("Failed to send verification code to email ID: '"
                    + toEmail + "'", e);
            throw ServiceException.FAILURE("Failed to send verification code to email ID: "
                    + toEmail, e);
        }
    }

    protected SendTwoFactorAuthCodeAction getAction() {
        return SendTwoFactorAuthCodeAction.EMAIL;
    }

}
