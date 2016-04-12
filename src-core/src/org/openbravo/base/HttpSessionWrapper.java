/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */

package org.openbravo.base;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

/**
 * Implements a fake HttpSession to be used when running out of a Servlet container (such as Quartz
 * scheduler or jUnit)
 * 
 * @author alostale
 *
 */
public class HttpSessionWrapper implements HttpSession {
  private Map<String, Object> attributes = new HashMap<String, Object>();

  @Override
  public Object getAttribute(String arg0) {
    return attributes.get(arg0);
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    return Collections.enumeration(attributes.keySet());
  }

  @Override
  public long getCreationTime() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String getId() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public long getLastAccessedTime() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getMaxInactiveInterval() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public ServletContext getServletContext() {
    return null;
  }

  /**
   * @deprecated
   */
  @Override
  public javax.servlet.http.HttpSessionContext getSessionContext() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * @deprecated
   */
  @Override
  public Object getValue(String arg0) {
    return null;
  }

  /**
   * @deprecated
   */
  @Override
  public String[] getValueNames() {
    return null;
  }

  @Override
  public void invalidate() {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean isNew() {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * @deprecated
   */
  @Deprecated
  @Override
  public void putValue(String arg0, Object arg1) {
    // TODO Auto-generated method stub

  }

  @Override
  public void removeAttribute(String arg0) {
    setAttribute(arg0, null);
  }

  /**
   * @deprecated
   */
  @Override
  public void removeValue(String arg0) {
    // TODO Auto-generated method stub
  }

  @Override
  public void setAttribute(String arg0, Object arg1) {
    attributes.put(arg0, arg1);
  }

  @Override
  public void setMaxInactiveInterval(int arg0) {
    // TODO Auto-generated method stub
  }
}
