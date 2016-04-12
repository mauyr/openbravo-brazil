/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2001-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.businessUtility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.sql.Connection;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.internet.MimeUtility;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.OrganizationEnabled;
import org.openbravo.client.application.window.AttachmentsAH;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.SecurityChecker;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.utils.FileUtility;
import org.openbravo.xmlEngine.XmlDocument;

public class TabAttachments extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private static Logger log = Logger.getLogger(TabAttachments.class);

  @Override
  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {

    final VariablesSecureApp vars = new VariablesSecureApp(request);
    post(vars, request, response);
  }

  public void post(VariablesSecureApp vars, HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    OBError myMessage = null;

    if (vars.getCommand().startsWith("SAVE_NEW")) {
      final String strTab = vars.getStringParameter("inpTabId");
      vars.setSessionValue("TabAttachments.tabId", strTab);
      final String strWindow = vars.getStringParameter("inpwindowId");
      vars.setSessionValue("TabAttachments.windowId", strWindow);
      final String key = vars.getStringParameter("inpKey");
      vars.setSessionValue("TabAttachments.key", key);
      final String strText = vars.getStringParameter("inpDescription");
      final String strDataType = vars.getStringParameter("inpadDatatypeId");
      final TabAttachmentsData[] data = TabAttachmentsData.selectTabInfo(this, strTab);
      String tableId = "";
      if (data == null || data.length == 0)
        throw new ServletException("Tab not found: " + strTab);
      else
        tableId = data[0].adTableId;

      final String documentOrganization = vars.getStringParameter("inpDocumentOrg");
      final String strFileReference = SequenceIdData.getUUID();
      final OBError oberrInsert = insert(vars, strFileReference, tableId, key, strDataType,
          strText, documentOrganization);
      if (!oberrInsert.getType().equals("Success")) {
        vars.setMessage("TabAttachments", oberrInsert);
        response.sendRedirect(strDireccion + request.getServletPath() + "?Command=DEFAULT");
      } else {
        // This command is only for 2.50
        if (vars.commandIn("SAVE_NEW_RELATION")) {
          response.sendRedirect(strDireccion + request.getServletPath()
              + "?Command=DEFAULT&inpcFileId=" + strFileReference);
          // This command is only for 2.50
        } else if (vars.commandIn("SAVE_NEW_EDIT")) {
          response.sendRedirect(strDireccion + request.getServletPath()
              + "?Command=EDIT&inpcFileId=" + strFileReference);
          // This command is only for 2.50
        } else if (vars.commandIn("SAVE_NEW_NEW")) {
          response.sendRedirect(strDireccion + request.getServletPath() + "?Command=NEW");
        } else if (vars.commandIn("SAVE_NEW_OB3")) {
          OBContext.setAdminMode();
          try {
            Tab tab = OBDal.getInstance().get(Tab.class, strTab);
            JSONObject obj = AttachmentsAH.getAttachmentJSONObject(tab, key);
            String buttonId = vars.getStringParameter("buttonId");
            response.setContentType("text/html; charset=UTF-8");
            Writer writer = response.getWriter();
            writer.write(getUploadFinishedScript(buttonId, obj));
          } finally {
            OBContext.restorePreviousMode();
          }
        }
      }
    } else if (vars.commandIn("EDIT_DESC_OB3")) {
      final String strText = vars.getStringParameter("inpDescription");
      vars.setSessionValue("TabAttachments.inpDescription", strText);
      final String attachId = vars.getStringParameter("inpAttachId");
      vars.setSessionValue("TabAttachments.buttonId", attachId);
      final String strTab = vars.getStringParameter("inpTabId");
      vars.setSessionValue("TabAttachments.tabId", strTab);
      final String key = vars.getStringParameter("inpKey");
      vars.setSessionValue("TabAttachments.key", key);
      editDescOB3(response, vars, attachId, strText, strTab, key);
      // This command is only for 2.50
    } else if (vars.getCommand().startsWith("SAVE_EDIT")) {
      final String strTab = vars.getStringParameter("inpTabId");
      vars.setSessionValue("TabAttachments.tabId", strTab);
      final String strWindow = vars.getStringParameter("inpwindowId");
      vars.setSessionValue("TabAttachments.windowId", strWindow);
      final String key = vars.getStringParameter("inpKey");
      vars.setSessionValue("TabAttachments.key", key);
      String strFileReference = vars.getStringParameter("inpcFileId");
      final String strDataType = vars.getStringParameter("inpadDatatypeId");
      final String strText = vars.getStringParameter("inptext");
      if (TabAttachmentsData.update(this, vars.getUser(), strDataType, strText, strFileReference) == 0) {
        myMessage = new OBError();
        myMessage.setType("Success");
        myMessage.setTitle("");
        myMessage.setMessage(Utility.messageBD(this, "Error", vars.getLanguage()));
        vars.setMessage("TabAttachments", myMessage);
        // vars.setSessionValue("TabAttachments.message",
        // Utility.messageBD(this, "Error", vars.getLanguage()));
        response.sendRedirect(strDireccion + request.getServletPath() + "?Command=EDIT&inpcFileId="
            + strFileReference);
      } else {
        if (vars.commandIn("SAVE_EDIT_RELATION")) {
          response.sendRedirect(strDireccion + request.getServletPath()
              + "?Command=DEFAULT&inpcFileId=" + strFileReference);
        } else if (vars.commandIn("SAVE_EDIT_EDIT")) {
          response.sendRedirect(strDireccion + request.getServletPath()
              + "?Command=EDIT&inpcFileId=" + strFileReference);
        } else if (vars.commandIn("SAVE_EDIT_NEW")) {
          response.sendRedirect(strDireccion + request.getServletPath() + "?Command=NEW&inpKey="
              + key);
        } else if (vars.commandIn("SAVE_EDIT_NEXT")) {
          final TabAttachmentsData[] data = TabAttachmentsData.selectTabInfo(this, strTab);
          String tableId = "";
          if (data == null || data.length == 0)
            throw new ServletException("Tab not found: " + strTab);
          else {
            tableId = data[0].adTableId;
            if (data[0].isreadonly.equals("Y"))
              throw new ServletException("This tab is read only");
          }
          final String strNewFile = TabAttachmentsData.selectNext(this,
              Utility.getContext(this, vars, "#User_Client", strWindow),
              Utility.getContext(this, vars, "#AccessibleOrgTree", strWindow), strFileReference,
              tableId, key);
          if (!strNewFile.equals(""))
            strFileReference = strNewFile;
          response.sendRedirect(strDireccion + request.getServletPath()
              + "?Command=EDIT&inpcFileId=" + strFileReference);
        }
      }
      // This command is only for 2.50
    } else if (vars.commandIn("DEL")) {
      final String strTab = vars.getStringParameter("inpTabId");
      vars.setSessionValue("TabAttachments.tabId", strTab);
      final String strWindow = vars.getStringParameter("inpwindowId");
      vars.setSessionValue("TabAttachments.windowId", strWindow);
      final String key = vars.getStringParameter("inpKey");
      vars.setSessionValue("TabAttachments.key", key);
      final String strFileReference = vars.getStringParameter("inpcFileId");
      final OBError oberrDelete = delete(vars, strFileReference);
      if (!oberrDelete.getType().equals("Success")) {
        vars.setMessage("TabAttachments", oberrDelete);
        // vars.setSessionValue("TabAttachments.message",
        // Utility.messageBD(this, "Error", vars.getLanguage()));
        // response.sendRedirect(strDireccion + request.getServletPath()
        // + "?Command=EDIT&inpcFileId=" + strFileReference);
        response.sendRedirect(strDireccion + request.getServletPath() + "?Command=DEFAULT");
      } else
        response.sendRedirect(strDireccion + request.getServletPath());
    } else if (vars.commandIn("DISPLAY_DATA")) {
      final String strFileReference = vars.getStringParameter("inpcFileId");
      printPageFile(response, vars, strFileReference, request);
    } else if (vars.getCommand().contains("GET_MULTIPLE_RECORDS_OB3")) {
      printPageFileMultiple(response, vars);
      // This command is only for 2.50
    } else if (vars.commandIn("DEFAULT")) {
      vars.getGlobalVariable("inpTabId", "TabAttachments.tabId");
      vars.getGlobalVariable("inpwindowId", "TabAttachments.windowId");
      vars.getGlobalVariable("inpKey", "TabAttachments.key");
      vars.getGlobalVariable("inpEditable", "TabAttachments.editable");
      printPageFS(response, vars);
      // This command is only for 2.50
    } else if (vars.commandIn("FRAME1", "RELATION")) {
      final String strTab = vars.getGlobalVariable("inpTabId", "TabAttachments.tabId");
      final String strWindow = vars.getGlobalVariable("inpwindowId", "TabAttachments.windowId");
      final String key = vars.getGlobalVariable("inpKey", "TabAttachments.key");
      final boolean editable = vars.getGlobalVariable("inpEditable", "TabAttachments.editable")
          .equals("Y");
      printPage(response, vars, strTab, strWindow, key, editable);
      // This command is only for 2.50
    } else if (vars.commandIn("FRAME2")) {
      whitePage(response);
      // This command is only for 2.50
    } else if (vars.commandIn("EDIT")) {
      final String strTab = vars.getGlobalVariable("inpTabId", "TabAttachments.tabId");
      final String strWindow = vars.getGlobalVariable("inpwindowId", "TabAttachments.windowId");
      final String key = vars.getGlobalVariable("inpKey", "TabAttachments.key");
      final String strFileReference = vars.getRequiredStringParameter("inpcFileId");
      printPageEdit(response, vars, strTab, strWindow, key, strFileReference);
      // This command is only for 2.50
    } else if (vars.commandIn("NEW")) {
      final String strTab = vars.getGlobalVariable("inpTabId", "TabAttachments.tabId");
      final String strWindow = vars.getGlobalVariable("inpwindowId", "TabAttachments.windowId");
      final String key = vars.getRequestGlobalVariable("inpKey", "TabAttachments.key");
      printPageEdit(response, vars, strTab, strWindow, key, "");
      // This command is only for 2.50
    } else if (vars.commandIn("DISPLAY_DATA")) {
      final String strFileReference = vars.getRequiredStringParameter("inpcFileId");
      printPageFile(response, vars, strFileReference, request);
      // This command is only for 2.50
    } else if (vars.commandIn("CHECK")) {
      final String tabId = vars.getStringParameter("inpTabId");
      final String inpKey = vars.getStringParameter("inpKey");
      printPageCheck(response, vars, tabId, inpKey);
    } else
      pageError(response);
  }

  private void printPageFileMultiple(HttpServletResponse response, VariablesSecureApp vars)
      throws IOException {
    OBContext.setAdminMode(true);
    try {
      String tabId = vars.getStringParameter("tabId");
      String recordIds = vars.getStringParameter("recordIds");
      Tab tab = OBDal.getInstance().get(Tab.class, tabId);
      String tableId = (String) DalUtil.getId(tab.getTable());

      // Checks if the user has readable access to the record where the file is attached
      Entity entity = ModelProvider.getInstance().getEntityByTableId(tableId);
      if (entity != null) {
        for (String recordId : recordIds.split(",")) {
          Object object = OBDal.getInstance().get(entity.getMappingClass(), recordId);
          if (object instanceof OrganizationEnabled) {
            SecurityChecker.getInstance().checkReadableAccess((OrganizationEnabled) object);
          }
        }
      }

      OBCriteria<Attachment> attachmentFiles = OBDao.getFilteredCriteria(Attachment.class,
          Restrictions.eq("table.id", tableId), Restrictions.in("record", recordIds.split(",")));

      attachmentFiles.setFilterOnReadableOrganization(false);
      response.setContentType("application/zip");
      response.setHeader("Content-Disposition", "attachment; filename=attachments.zip");
      final ZipOutputStream dest = new ZipOutputStream(response.getOutputStream());
      HashMap<String, Integer> writtenFiles = new HashMap<String, Integer>();
      for (Attachment attachmentFile : attachmentFiles.list()) {
        String attachmentDirectory = TabAttachments.getAttachmentDirectory(tableId,
            attachmentFile.getRecord(), attachmentFile.getName());
        final File file = new File(globalParameters.strFTPDirectory + "/" + attachmentDirectory,
            attachmentFile.getName());
        String zipName = "";
        if (!writtenFiles.containsKey(file.getName())) {
          zipName = file.getName();
          writtenFiles.put(file.getName(), new Integer(0));
        } else {
          int num = writtenFiles.get(file.getName()) + 1;
          int indDot = file.getName().lastIndexOf(".");
          if (indDot == -1) {
            // file has no extension
            indDot = file.getName().length();
          }
          zipName = file.getName().substring(0, indDot) + " (" + num + ")"
              + file.getName().substring(indDot);
          writtenFiles.put(file.getName(), new Integer(num));
        }
        byte[] buf = new byte[1024];
        dest.putNextEntry(new ZipEntry(zipName));
        FileInputStream in = new FileInputStream(file.toString());
        int len;
        while ((len = in.read(buf)) > 0) {
          dest.write(buf, 0, len);
        }
        dest.closeEntry();
        in.close();
      }
      dest.close();
    } catch (Exception e) {
      log4j.error("Error while downloading attachments", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private OBError insert(VariablesSecureApp vars, String strFileReference, String tableId,
      String key, String strDataType, String strText, String documentOrganization)
      throws IOException, ServletException {

    String cFileId = strFileReference, fileDir = null, path = null;
    OBError myMessage = null;
    myMessage = new OBError();
    myMessage.setTitle("");

    if (log4j.isDebugEnabled())
      log4j.debug("Deleting records");
    Connection conn = null;
    fileDir = getAttachmentDirectoryForNewAttachments(tableId, key);
    try {
      conn = this.getTransactionConnection();
      final String inpName = "inpname";
      String strName = "";
      final FileItem file = vars.getMultiFile(inpName);
      if (file == null)
        throw new ServletException("Empty file");
      strName = file.getName();
      // FIXME: Get the directory separator from Java runtime
      int i = strName.lastIndexOf("\\");
      if (i != -1) {
        strName = strName.substring(i + 1);
        // FIXME: Get the directory separator from Java runtime
      } else if ((i = strName.lastIndexOf("/")) != -1) {
        strName = strName.substring(i + 1);
      }
      boolean fileExists = false;
      final TabAttachmentsData[] files = TabAttachmentsData.select(this, "'" + vars.getClient()
          + "'", "'" + documentOrganization + "'", tableId, key);
      for (TabAttachmentsData data : files) {
        if (data.name.equals(strName)) {
          fileExists = true;
          cFileId = data.cFileId;
        }
      }
      if (!fileExists) {
        path = getPath(fileDir);
        // We only insert a new record if there is no record for this file
        TabAttachmentsData.insert(conn, this, cFileId, vars.getClient(), documentOrganization,
            vars.getUser(), tableId, key, strDataType, strText, strName, path);
      } else {
        // We update the existing record
        TabAttachmentsData.update(this, vars.getUser(), strDataType, strText, cFileId);
      }
      try {
        // FIXME: Get the directory separator from Java runtime

        final File uploadedDir = new File(globalParameters.strFTPDirectory + "/" + fileDir);
        if (!uploadedDir.exists())
          uploadedDir.mkdirs();
        final File uploadedFile = new File(uploadedDir, strName);
        file.write(uploadedFile);
        // FIXME: We should be closing the file here to make sure that
        // is closed
        // and that is does not really get closed when the GC claims the
        // object (indeterministic)
      } catch (final Exception ex) {
        throw new ServletException(ex);
      }
      releaseCommitConnection(conn);
    } catch (final Exception e) {
      try {
        releaseRollbackConnection(conn);
      } catch (final Exception ignored) {
      }
      e.printStackTrace();
      log4j.error("Rollback in transaction");
      myMessage.setType("Error");
      myMessage.setMessage(Utility.messageBD(this, "ProcessRunError", vars.getLanguage()));
      return myMessage;
      // return "ProcessRunError";
    }
    myMessage.setType("Success");
    myMessage.setMessage(Utility.messageBD(this, "Success", vars.getLanguage()));
    return myMessage;
    // return "";
  }

  private OBError delete(VariablesSecureApp vars, String strFileReference) throws IOException,
      ServletException {
    OBError myMessage = null;
    myMessage = new OBError();
    myMessage.setTitle("");

    if (log4j.isDebugEnabled())
      log4j.debug("Deleting records");
    Connection conn = null;
    try {
      conn = this.getTransactionConnection();
      final TabAttachmentsData[] data = TabAttachmentsData.selectReference(this, strFileReference);
      TabAttachmentsData.delete(conn, this, strFileReference);
      try {
        FileUtility f = new FileUtility();
        // FIXME: Get the directory separator from Java runtime
        String attachmentDirectory = TabAttachments.getAttachmentDirectory(data[0].adTableId,
            data[0].adRecordId, data[0].name);
        final File file = new File(globalParameters.strFTPDirectory + "/" + attachmentDirectory,
            data[0].name);
        if (file.exists())
          f = new FileUtility(globalParameters.strFTPDirectory + "/" + attachmentDirectory,
              data[0].name, false);
        else
          f = new FileUtility(globalParameters.strFTPDirectory, strFileReference, false);
        if (!f.deleteFile()) {
          myMessage.setType("Error");
          myMessage.setMessage(Utility.messageBD(this, "ProcessRunError", vars.getLanguage()));
          return myMessage;
        }
      } catch (final Exception ex) {
        throw new ServletException(ex);
      }
      releaseCommitConnection(conn);
    } catch (final Exception e) {
      try {
        releaseRollbackConnection(conn);
      } catch (final Exception ignored) {
      }
      e.printStackTrace();
      log4j.error("Rollback in transaction");
      myMessage.setType("Error");
      myMessage.setMessage(Utility.messageBD(this, "ProcessRunError", vars.getLanguage()));
      return myMessage;
      // return "ProcessRunError";
    }
    myMessage.setType("Success");
    myMessage.setMessage(Utility.messageBD(this, "Success", vars.getLanguage()));
    return myMessage;
    // return "";
  }

  private void printPageFS(HttpServletResponse response, VariablesSecureApp vars)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: Attachments relations frame set");
    final XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/businessUtility/TabAttachments_FS").createXmlDocument();

    response.setContentType("text/html; charset=UTF-8");
    final PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strTab,
      String strWindow, String key, boolean editable) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: Frame 1 of the attachments relations");
    final String[] discard = { "noData", "" };
    if (!editable)
      discard[1] = "editable";
    final TabAttachmentsData[] data = TabAttachmentsData.selectTabInfo(this, strTab);
    String tableId = "";
    if (data == null || data.length == 0)
      throw new ServletException("Tab not found: " + strTab);
    else {
      tableId = data[0].adTableId;
      if (data[0].isreadonly.equals("Y"))
        discard[0] = new String("selReadOnly");
    }

    final TabAttachmentsData[] files = TabAttachmentsData.select(this,
        Utility.getContext(this, vars, "#User_Client", strWindow),
        Utility.getContext(this, vars, "#AccessibleOrgTree", strWindow), tableId, key);

    if ((files == null) || (files.length == 0))
      discard[0] = "widthData";
    final XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/businessUtility/TabAttachments_F1", discard).createXmlDocument();

    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("tab", strTab);
    xmlDocument.setParameter("window", strWindow);
    xmlDocument.setParameter("key", key);
    xmlDocument.setParameter("recordIdentifier",
        TabAttachmentsData.selectRecordIdentifier(this, key, vars.getLanguage(), strTab));

    {
      final OBError myMessage = vars.getMessage("TabAttachments");
      vars.removeMessage("TabAttachments");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setData("structure1", files);

    response.setContentType("text/html; charset=UTF-8");
    final PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void printPageEdit(HttpServletResponse response, VariablesSecureApp vars, String strTab,
      String strWindow, String key, String strFileReference) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: Frame 1 of the attachments edition");
    final String[] discard = { "editDiscard" };
    final TabAttachmentsData[] data = TabAttachmentsData.selectTabInfo(this, strTab);
    if (data == null || data.length == 0)
      throw new ServletException("Tab not found: " + strTab);
    else {
      if (data[0].isreadonly.equals("Y"))
        throw new ServletException("This tab is read only");
    }
    if (strFileReference.equals(""))
      discard[0] = new String("newDiscard");
    final XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/businessUtility/TabAttachments_Edition", discard)
        .createXmlDocument();

    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("tab", strTab);
    xmlDocument.setParameter("window", strWindow);
    xmlDocument.setParameter("key", key);
    xmlDocument.setParameter("save", (strFileReference.equals("") ? "NEW" : "EDIT"));
    xmlDocument.setParameter("recordIdentifier",
        TabAttachmentsData.selectRecordIdentifier(this, key, vars.getLanguage(), strTab));

    {
      final OBError myMessage = vars.getMessage("TabAttachments");
      vars.removeMessage("TabAttachments");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    /*
     * String message = vars.getSessionValue("TabAttachments.message"); if (!message.equals(""))
     * message = "alert('" + message + "');"; xmlDocument.setParameter("body", message);
     */

    final TabAttachmentsData[] files = TabAttachmentsData.selectEdit(this, strFileReference);
    // FIXME: If we do not use this code, it should be removed
    /*
     * String viewButtons = "yes"; if (strFileReference.equals("")||files==null||files.length==0)
     * viewButtons="none";
     * 
     * xmlDocument.setParameter("butEdit", viewButtons); xmlDocument.setParameter("butDownload",
     * viewButtons); xmlDocument.setParameter("butDel", viewButtons);
     */

    xmlDocument.setData("structure1", (strFileReference.equals("") ? TabAttachmentsData.set()
        : files));
    xmlDocument.setData(
        "reportAD_Datatype_ID_D",
        "liststructure",
        DataTypeComboData.select(this,
            Utility.getContext(this, vars, "#User_Client", "TabAttachments"),
            Utility.getContext(this, vars, "#AccessibleOrgTree", "TabAttachments")));

    response.setContentType("text/html; charset=UTF-8");
    final PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void printPageFile(HttpServletResponse response, VariablesSecureApp vars,
      String strFileReference, HttpServletRequest request) throws IOException, ServletException {
    String fileDir = null;
    final TabAttachmentsData[] data = TabAttachmentsData.selectEdit(this, strFileReference);

    // Checks if the user has readable access to the record where the file is attached
    Entity entity = ModelProvider.getInstance().getEntityByTableId(data[0].adTableId);
    if (entity != null) {
      Object object = OBDal.getInstance().get(entity.getMappingClass(), data[0].adRecordId);
      if (object instanceof OrganizationEnabled) {
        SecurityChecker.getInstance().checkReadableAccess((OrganizationEnabled) object);
      }
    }

    if (data == null || data.length == 0)
      throw new ServletException("Missing file");
    FileUtility f = new FileUtility();
    fileDir = getAttachmentDirectory(data[0].adTableId, data[0].adRecordId, data[0].name);
    // FIXME: Get the directory separator from Java runtime
    final File file = new File(globalParameters.strFTPDirectory + "/" + fileDir, data[0].name);
    if (file.exists())
      f = new FileUtility(globalParameters.strFTPDirectory + "/" + fileDir, data[0].name, false,
          true);
    else
      f = new FileUtility(globalParameters.strFTPDirectory, strFileReference, false, true);
    if (data[0].datatypeContent.equals(""))
      response.setContentType("application/txt");
    else
      response.setContentType(data[0].datatypeContent);
    response.setCharacterEncoding("UTF-8");
    String userAgent = request.getHeader("user-agent");
    if (userAgent.contains("MSIE")) {
      response.setHeader(
          "Content-Disposition",
          "attachment; filename=\""
              + URLEncoder.encode(data[0].name.replace("\"", "\\\""), "utf-8") + "\"");
    } else {
      response.setHeader(
          "Content-Disposition",
          "attachment; filename=\""
              + MimeUtility.encodeWord(data[0].name.replace("\"", "\\\""), "utf-8", "Q") + "\"");
    }

    f.dumpFile(response.getOutputStream());
    response.getOutputStream().flush();
    response.getOutputStream().close();
  }

  private void printPageCheck(HttpServletResponse response, VariablesSecureApp vars, String strTab,
      String recordId) throws IOException, ServletException {
    response.setContentType("text/plain; charset=UTF-8");
    final PrintWriter out = response.getWriter();
    out.print(Utility.hasTabAttachments(this, vars, strTab, recordId));
    out.close();
  }

  private void editDescOB3(HttpServletResponse response, VariablesSecureApp vars,
      String attachmentId, String desc, String strTab, String key) {
    OBContext.setAdminMode(false);
    try {
      Attachment attachment = OBDal.getInstance().get(Attachment.class, attachmentId);

      attachment.setText(desc);
      OBDal.getInstance().save(attachment);
      OBDal.getInstance().flush();
      OBDal.getInstance().getConnection().commit();
      Tab tab = OBDal.getInstance().get(Tab.class, strTab);
      String tableId = (String) DalUtil.getId(tab.getTable());

      // Checks if the user has readable access to the record where the file is attached
      Entity entity = ModelProvider.getInstance().getEntityByTableId(tableId);
      if (entity != null) {
        Object object = OBDal.getInstance().get(entity.getMappingClass(), key);
        if (object instanceof OrganizationEnabled) {
          SecurityChecker.getInstance().checkReadableAccess((OrganizationEnabled) object);
        }
      }

      JSONObject obj = AttachmentsAH.getAttachmentJSONObject(tab, key);
      String buttonId = vars.getStringParameter("buttonId");
      response.setContentType("text/html; charset=UTF-8");
      Writer writer = response.getWriter();
      writer.write(getUploadFinishedScript(buttonId, obj));
    } catch (Exception e) {
      log4j.debug("Problem saving attachment: " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private String getUploadFinishedScript(String buttonId, JSONObject obj) {
    StringBuilder script = new StringBuilder();
    script.append("<HTML><BODY><script type=\"text/javascript\">");
    script.append("var iscWindow = top.OB || parent.OB;\n");
    script.append("iscWindow.Utilities.uploadFinished(\"" + buttonId + "\"," + obj.toString()
        + ");");
    script.append("</SCRIPT></BODY></HTML>");
    return script.toString();
  }

  /**
   * Provides the directory in which the attachment has to be stored. For example for tableId "259",
   * recordId "0F3A10E019754BACA5844387FB37B0D5", the file directory returned is
   * "259/0F3/A10/E01/975/4BA/CA5/844/387/FB3/7B0/D5". In case 'SaveAttachmentsOldWay' preference is
   * enabled then the file directory returned is "259-0F3A10E019754BACA5844387FB37B0D5"
   * 
   * @param tableID
   *          UUID of the table
   * 
   * @param recordID
   *          UUID of the record
   * 
   * @return file directory to save the attachment
   */
  public static String getAttachmentDirectoryForNewAttachments(String tableID, String recordID) {
    String fileDir = tableID + "-" + recordID;
    String saveAttachmentsOldWay = null;
    try {
      saveAttachmentsOldWay = Preferences.getPreferenceValue("SaveAttachmentsOldWay", true,
          OBContext.getOBContext().getCurrentClient(), OBContext.getOBContext()
              .getCurrentOrganization(), OBContext.getOBContext().getUser(), OBContext
              .getOBContext().getRole(), null);
    } catch (PropertyException e) {
      // if property not found, save attachments the new way
      saveAttachmentsOldWay = "N";
    }

    if ("Y".equals(saveAttachmentsOldWay)) {
      return fileDir;
    } else {
      fileDir = tableID + "/" + splitPath(recordID);
    }
    return fileDir;
  }

  /**
   * Provides the directory in which the attachment is stored. For example for tableId "259",
   * recordId "0F3A10E019754BACA5844387FB37B0D5", and fileName "test.txt" the file directory
   * returned is "259/0F3/A10/E01/975/4BA/CA5/844/387/FB3/7B0/D5". In case 'SaveAttachmentsOldWay'
   * preference is enabled then the file directory returned is
   * "259-0F3A10E019754BACA5844387FB37B0D5"
   * 
   * @param tableID
   *          UUID of the table
   * 
   * @param recordID
   *          UUID of the record
   * 
   * @param fileName
   *          Name of the file
   * 
   * @return file directory in which the attachment is stored
   */
  public static String getAttachmentDirectory(String tableID, String recordID, String fileName) {
    String fileDir = tableID + "-" + recordID;
    Table attachmentTable = null;
    try {
      OBContext.setAdminMode();
      attachmentTable = OBDal.getInstance().get(Table.class, tableID);
      OBCriteria<Attachment> attachmentCriteria = OBDal.getInstance().createCriteria(
          Attachment.class);
      attachmentCriteria.add(Restrictions.eq(Attachment.PROPERTY_RECORD, recordID));
      attachmentCriteria.add(Restrictions.eq(Attachment.PROPERTY_TABLE, attachmentTable));
      attachmentCriteria.add(Restrictions.eq(Attachment.PROPERTY_NAME, fileName));

      attachmentCriteria.setFilterOnReadableOrganization(false);
      if (attachmentCriteria.count() > 0) {
        Attachment attachment = attachmentCriteria.list().get(0);
        if (attachment.getPath() != null) {
          fileDir = attachment.getPath();
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return fileDir;
  }

  /**
   * Provides the value to be saved in path field in c_file. The path field is used to get the
   * location of the attachment. For example 259/0F3/A10/E01/975/4BA/CA5/844/387/FB3/7B0/D5. This
   * path is relative to the attachments folder
   * 
   * @param fileDirectory
   *          the directory that is retrieved from getFileDirectory()
   * 
   * @return value to be saved in path in c_file
   */
  public static String getPath(String fileDirectory) {
    if (fileDirectory != null && fileDirectory.contains("-")) {
      return null;
    } else {
      return fileDirectory;
    }
  }

  /**
   * Splits the path name component so that the resulting path name is 3 characters long sub
   * directories. For example 12345 is splitted to 123/45
   * 
   * @param origname
   *          Original name
   * @return splitted name.
   */
  public static String splitPath(final String origname) {
    String newname = "";
    for (int i = 0; i < origname.length(); i += 3) {
      if (i != 0) {
        newname += "/";
      }
      newname += origname.substring(i, Math.min(i + 3, origname.length()));
    }
    return newname;
  }

  @Override
  public String getServletInfo() {
    return "Servlet that presents the attachments";
  } // end of getServletInfo() method
}
