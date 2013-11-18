/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.qa.management.ws.cli;

import static org.jboss.qa.management.ws.cli.CLIWebservicesWsdlPortIT.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.qa.management.ws.BaseDeployment.WarDeployment;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.After;
import org.junit.Before;

/**
 * see https://docspace.corp.redhat.com/docs/DOC-152480
 *
 * set wsdl-port to different value (8084) then value in wsdl file (8081)
 *
 */
public final class CLIWebservicesModifyWsdlAddressIT extends CLITestCase
{
   //private static final String MODIFY_WSDL_ADDRESS = "true";
   private static final String MODIFY_WSDL_ADDRESS_DEFAULT = "true";
   private static final String MODIFY_WSDL_ADDRESS_CHANGED = "false";
   private static final String MODIFY_WSDL_ADDRESS_INVALID = "invalid";
   private static final String MODIFY_WSDL_ADDRESS_UNDEFINED = "true";

   private static final int WSDL_PORT_DEFAULT = 8080;
   private static final int WSDL_PORT_PRECONFIGURED = 8084;
   private static final int WSDL_PORT_SPECIFIED_IN_WSDL = 8081;

   static final String NAME = "CLIWebservicesModifyWsdlAddressTestCase";
   static final String NAME2 = "CLIWebservicesModifyWsdlAddressTestCase2";

   public CLIWebservicesModifyWsdlAddressIT()
   {
      super("/subsystem=webservices/:read-attribute(name=modify-wsdl-address)",
            "/subsystem=webservices/:write-attribute(name=modify-wsdl-address,value=" + MODIFY_WSDL_ADDRESS_CHANGED + ")",
            "/subsystem=webservices/:write-attribute(name=modify-wsdl-address,value=\"" + MODIFY_WSDL_ADDRESS_DEFAULT + "\")",
            "/subsystem=webservices/:undefine-attribute(name=modify-wsdl-address)",
            new String [] {"/subsystem=webservices/:write-attribute(name=modify-wsdl-address,value=" + MODIFY_WSDL_ADDRESS_INVALID + ")"},
            createWarDeployment(NAME + WAR_EXTENSTION).createArchive(),
            createWarDeployment(NAME2 + WAR_EXTENSTION).createArchive()
            );
   }

   @Before
   public void before() throws Exception {
      info("Before");
      executeAssertedCLICommand("/subsystem=webservices/:write-attribute(name=wsdl-port,value=" + WSDL_PORT_PRECONFIGURED + ")");
      reloadServer();
   }

   @Override
   @After
   public void after() throws Exception
   {
      executeCLICommandQuietly("/subsystem=webservices/:write-attribute(name=wsdl-port,value=" + WSDL_PORT_DEFAULT + ")");
      super.after();
   }

   static WarDeployment createWarDeployment(String name) {
      return new WarDeployment(name) { {
         archive
            .setManifest(new StringAsset("Manifest-Version: 1.0\n"
                  + "Dependencies: org.jboss.ws.cxf.jbossws-cxf-client\n"))
            .addClass(org.jboss.qa.management.ws.cli.AnnotatedServiceWithStaticWsdlIface.class)
            .addClass(org.jboss.qa.management.ws.cli.AnnotatedServiceWithStaticWsdlImpl.class)
            .addClass(org.jboss.qa.management.ws.cli.SayHello.class)
            .addClass(org.jboss.qa.management.ws.cli.SayHelloResponse.class)
            .addAsManifestResource(createResourcePrefix() + "endpoint.wsdl", "endpoint.wsdl")
            ;
      } };
   }

   private static String createResourcePrefix()
   {
      return CLIWebservicesModifyWsdlAddressIT.class.getName().toLowerCase().replace(".", "/") + "/";
   }

   private URL createWsdlUrl(String name) throws MalformedURLException
   {
      return new URL(createServiceURL(name) + "?wsdl");
   }

   @Override
   protected void assertDefaultConfigurationValue(CLIResult result)
   {
      result.assertResultAsStringEquals(MODIFY_WSDL_ADDRESS_DEFAULT);
   }

   private String createServiceURL(String contextName)
   {
      return createServiceURL(contextName, PORT);
   }

   private String createServiceURL(String contextName, int port)
   {
      return createServiceURL("localhost", contextName, port); ///*JBossWSTestHelper.getServerHost()*/
   }

   String createServiceURL(String wsdlHost, String contextName, int port)
   {
      return "http://" + wsdlHost + ":" + port + "/" + contextName + "/AnnotatedSecurityService";
   }


   @Override
   protected void assertOriginalConfiguration(String contextName) throws UnsupportedEncodingException, IOException, MalformedURLException
   {
      assertCorrectWsdlReturned(readFromUrlToString(createWsdlUrl(contextName)), contextName, WSDL_PORT_PRECONFIGURED);
      // assertServiceIsFunctional(createServiceURL(contextName, WSDL_PORT_CHANGED)); will not work with wdl_port rewritten to wrong value
   }

   @Override
   protected void assertUndefinedConfiguration(String contextName) throws UnsupportedEncodingException, IOException, MalformedURLException
   {
      assertCorrectWsdlReturned(readFromUrlToString(createWsdlUrl(contextName)), contextName, WSDL_PORT_PRECONFIGURED);
      // assertServiceIsFunctional(createServiceURL(contextName, WSDL_PORT_CHANGED)); will not work with wdl_port rewritten to wrong value
   }

   @Override
   protected void assertChangedConfiguration(String contextName) throws UnsupportedEncodingException, IOException, MalformedURLException
   {
      assertCorrectWsdlReturned(readFromUrlToString(createWsdlUrl(contextName)), contextName, WSDL_PORT_SPECIFIED_IN_WSDL);
      // assertServiceIsFunctional(createServiceURL(contextName, WSDL_PORT_CHANGED)); will not work with wdl_port rewritten to wrong value
   }

   protected void assertCorrectWsdlReturned(String wsdl, String contextName, int wsdlPort)
   {
      assertTrue(wsdl.contains("sayHelloResponse"));
      String expectedSoapAddress = SOAP_ADDRESS_LOCATION_PREFIX + createServiceURL(CLIWebservicesWsdlHostIT.WSDL_HOST, contextName, wsdlPort) + "\"/>";
      assertEquals(expectedSoapAddress, findSoapAddress(wsdl));
   }

   @Override
   protected void assertChangeConfigurationResult(CLIResult result)
   {
      result.assertReloadRequired();
   }

   @Override
   protected void assertChangedConfigurationValue(CLIResult result)
   {
      result.assertResultAsStringEquals(MODIFY_WSDL_ADDRESS_CHANGED);
   }

   @Override
   protected void assertUndefinedConfigurationValue(CLIResult result)
   {
      result.assertResultAsStringEquals(MODIFY_WSDL_ADDRESS_UNDEFINED);
   }
}
