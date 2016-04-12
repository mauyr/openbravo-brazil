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
 * All portions are Copyright (C) 2010-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.base.weld.test;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.KernelInitializer;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.test.base.OBBaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base test for weld, provides access to the weld container.
 * 
 * @author mtaal
 */
@RunWith(Arquillian.class)
public class WeldBaseTest extends OBBaseTest {
  private static final Logger log = LoggerFactory.getLogger(WeldBaseTest.class);

  private static boolean initialized = false;
  private static JavaArchive archive = null;

  @Deployment
  public static JavaArchive createTestArchive() {
    if (archive == null) {
      log.info("Creating cdi archive...");
      final String sourcePath = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty("source.path");
      archive = ShrinkWrap.create(JavaArchive.class);

      // add all beans without exclusions so cdi can also be used for *test* packages
      archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

      // include all classes deployed in webapp container
      archive.as(ExplodedImporter.class).importDirectory(sourcePath + "/build/classes/");

      // ...and all the jUnit ones
      archive.as(ExplodedImporter.class).importDirectory(sourcePath + "/src-test/build/classes/");

      // include all libraries deployed in webapp container
      archive.addAsDirectory(sourcePath + "/WebContent/WEB-INF/lib");

      log.debug(archive.toString(true));
      log.info("... cdi archive created");
    }
    return archive;
  }

  @SuppressWarnings("serial")
  private static final AnnotationLiteral<Any> ANY = new AnnotationLiteral<Any>() {
  };

  @Inject
  private BeanManager beanManager;

  @Inject
  private KernelInitializer kernelInitializer;

  /**
   * Sets static instance bean manager in WeldUtils so it is globally accessible and initializes
   * kernel.
   * 
   * Arquillian creates a new cdi container for each test class but keeps existent one for all tests
   * within same class, let's initialize it once per class but we cannot use @BeforeClass at this
   * point because we require of beanManager to be injected.
   */
  @Before
  public void setManager() {
    if (!initialized) {
      WeldUtils.setStaticInstanceBeanManager(beanManager);
      kernelInitializer.setInterceptor();
      initialized = true;
    }
  }

  /**
   * Once we are done with the class execution, OBInterceptor needs to be reset other case when
   * executing a suite it will reuse the container created for the previous classes instead of the
   * new one.
   */
  @AfterClass
  public static void resetOBInterceptors() {
    final OBInterceptor interceptor = (OBInterceptor) SessionFactoryController.getInstance()
        .getConfiguration().getInterceptor();
    interceptor.setInterceptorListener(null);
    initialized = false;
  }

  @SuppressWarnings("unchecked")
  protected <U extends Object> U getWeldComponent(Class<U> clz) {

    final Bean<?> bean = beanManager.getBeans(clz, ANY).iterator().next();

    return (U) beanManager.getReference(bean, clz, beanManager.createCreationalContext(bean));
  }
}
