/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2013 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.email;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.poc.EmailManager;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.utils.FormatUtilities;

/**
 * This singleton class, is in charge of generating events to send emails.
 * 
 * @author asier
 * @see EmailEventContentGenerator
 * 
 */
@ApplicationScoped
public class EmailEventManager {

  private static final Logger log = Logger.getLogger(EmailEventManager.class);

  @Inject
  @Any
  private Instance<EmailEventContentGenerator> emailGenerators;

  /**
   * This method is invoked when an event for sending emails is generated. It looks for all
   * {@link EmailEventContentGenerator} classes listening to this event and generates an email using
   * them.
   * 
   * @param event
   *          Name of the event to send emails for
   * @param recipient
   *          Email address of the email's recipient
   * @param data
   *          Object that the EmailEventContentGenerator will receive to generate the email
   * @return <code>true</code> in case at least one email has been sent
   * @throws EmailEventException
   *           is thrown in case of problems sending the email or getting the email server
   *           configuration
   * 
   * @see EmailEventContentGenerator
   */
  public boolean sendEmail(String event, final String recipient, Object data)
      throws EmailEventException {
    // Retrieves the Email Server configuration
    Organization currenctOrg = OBContext.getOBContext().getCurrentOrganization();
    final EmailServerConfiguration mailConfig = EmailUtils.getEmailConfiguration(currenctOrg);

    if (mailConfig == null) {
      log.warn("Couldn't find email configuarion");
      throw new EmailEventException(OBMessageUtils.getI18NMessage("EmailConfigurationNotFound",
          null));
    }

    try {
      final String username = mailConfig.getSmtpServerAccount();
      final String password = FormatUtilities.encryptDecrypt(mailConfig.getSmtpServerPassword(),
          false);
      final String connSecurity = mailConfig.getSmtpConnectionSecurity();
      final int port = mailConfig.getSmtpPort().intValue();
      final String senderAddress = mailConfig.getSmtpServerSenderAddress();
      final String host = mailConfig.getSmtpServer();
      final boolean auth = mailConfig.isSMTPAuthentification();

      boolean sent = false;
      for (EmailEventContentGenerator gen : getEmailGenerators(event, data)) {
        sent = true;
        log.debug("sending email for event " + event + " with generator " + gen);

        if (gen.isAsynchronous()) {
          final String subject = gen.getSubject(data, event);
          final String body = gen.getBody(data, event);
          final String type = gen.getContentType();
          final List<File> attachments = gen.getAttachments(data, event);
          Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
              try {
                EmailManager.sendEmail(host, auth, username, password, connSecurity, port,
                    senderAddress, recipient, null, null, null, subject, body, type, attachments,
                    null, null);
              } catch (Exception e) {
                log.error(e.getMessage(), e);
              }
            }
          });
          thread.start();
        } else {
          EmailManager.sendEmail(host, auth, username, password, connSecurity, port, senderAddress,
              recipient, null, null, null, gen.getSubject(data, event), gen.getBody(data, event),
              gen.getContentType(), gen.getAttachments(data, event), null, null);
        }
      }
      if (!sent) {
        log.warn("No email generator found for event " + event);
      }
      return sent;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new EmailEventException(e);
    }
  }

  private List<EmailEventContentGenerator> getEmailGenerators(String event, Object data) {
    // find valid events
    List<EmailEventContentGenerator> validGenerators = new ArrayList<EmailEventContentGenerator>();
    Iterator<EmailEventContentGenerator> i = emailGenerators.iterator();
    while (i.hasNext()) {
      EmailEventContentGenerator gen = i.next();
      if (gen.isValidEvent(event, data)) {
        validGenerators.add(gen);
      }
    }

    // sort them by priority
    Collections.sort(validGenerators, new Comparator<EmailEventContentGenerator>() {
      @Override
      public int compare(EmailEventContentGenerator o1, EmailEventContentGenerator o2) {
        return o1.getPriority() - o2.getPriority();
      }
    });

    // if some of them prevents following execution, stop chain
    List<EmailEventContentGenerator> generators = new ArrayList<EmailEventContentGenerator>();
    for (EmailEventContentGenerator gen : validGenerators) {
      generators.add(gen);
      if (gen.preventsOthersExecution()) {
        break;
      }
    }

    return generators;
  }

}
