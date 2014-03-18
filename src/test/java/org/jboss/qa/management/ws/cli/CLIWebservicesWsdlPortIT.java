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

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.qa.management.ws.BaseDeployment.WarDeployment;
import org.jboss.shrinkwrap.api.asset.StringAsset;
/**
 *
 * see https://docspace.corp.redhat.com/docs/DOC-152480
 *
 */
public final class CLIWebservicesWsdlPortIT extends CLITestCase
{
  private static final String UNDEFINE_CONFIGURATION_CLI_COMMAND = "/subsystem=webservices/:undefine-attribute(name=wsdl-port)";
   static final String RESET_CONFIGURATION_CLI_COMMAND = UNDEFINE_CONFIGURATION_CLI_COMMAND;
  static final int PORT = 8080;
   static final int WSDL_PORT = PORT;
   static final int WSDL_PORT_CHANGED = 8084;
   private static final String WSDL_PORT_UNDEFINED = "undefined";
   private static final String WSDL_PORT_INVALID = "invalid";
   private static final int WSDL_PORT_INVALID_2 = -1;

   private static final String NAME = "CLIWebservicesWsdlPortIT";
   private static final String NAME2 = "CLIWebservicesWsdlPortIT2";


   static WarDeployment createWarDeployment(String name) {
      return new WarDeployment(name) { {
         archive
            .setManifest(new StringAsset("Manifest-Version: 1.0\n"
                  + "Dependencies: org.jboss.ws.cxf.jbossws-cxf-client\n"))
            .addClass(org.jboss.qa.management.ws.cli.AnnotatedServiceIface.class)
            .addClass(org.jboss.qa.management.ws.cli.AnnotatedServiceImpl.class)
            .addClass(org.jboss.qa.management.ws.cli.SayHello.class)
            .addClass(org.jboss.qa.management.ws.cli.SayHelloResponse.class)
            ;
      } };
   }

   public CLIWebservicesWsdlPortIT()
   {
      super("/subsystem=webservices/:read-attribute(name=wsdl-port)",
            "/subsystem=webservices/:write-attribute(name=wsdl-port,value=" + WSDL_PORT_CHANGED + ")",
            RESET_CONFIGURATION_CLI_COMMAND,
            UNDEFINE_CONFIGURATION_CLI_COMMAND,
            new String [] {"/subsystem=webservices/:write-attribute(name=wsdl-port,value=" + WSDL_PORT_INVALID + ")",
               "/subsystem=webservices/:write-attribute(name=wsdl-port,value=" + WSDL_PORT_INVALID_2 + ")"
            },
            createWarDeployment(NAME + WAR_EXTENSTION).createArchive(),
            createWarDeployment(NAME2 + WAR_EXTENSTION).createArchive()
            );
   }

   private URL createWsdlUrl(String name) throws MalformedURLException
   {
      return new URL(createServiceURL(name) + "?wsdl");
   }

   @Override
   protected void assertDefaultConfigurationValue(CLIResult result)
   {
     result.assertResultAsStringEquals(WSDL_PORT_UNDEFINED);
   }

   private String createServiceURL(String contextName)
   {
      return createServiceURL(contextName, PORT);
   }

   private String createServiceURL(String contextName, int port)
   {
      return "http://" + "localhost"/*JBossWSTestHelper.getServerHost()*/ + ":" + port + "/" + contextName + "/AnnotatedSecurityService";
   }


   @Override
   protected void assertOriginalConfiguration(String contextName) throws UnsupportedEncodingException, IOException, MalformedURLException
   {
      assertCorrectWsdlReturned(readFromUrlToString(createWsdlUrl(contextName)), contextName, WSDL_PORT);
      if (testIfServiceIsFunctional())
        assertServiceIsFunctional(createServiceURL(contextName));
   }

   @Override
   protected void assertUndefinedConfiguration(String contextName) throws UnsupportedEncodingException, IOException, MalformedURLException
   {
      assertCorrectWsdlReturned(readFromUrlToString(createWsdlUrl(contextName)), contextName, WSDL_PORT);
      if (testIfServiceIsFunctional())
        assertServiceIsFunctional(createServiceURL(contextName));
   }


   private void assertCorrectWsdlReturned(String wsdl, String contextName, int wsdlPort)
   {
      assertTrue(wsdl.contains("sayHelloResponse"));
      String expectedSoapAddress = "<soap:address location=\"" + createServiceURL(contextName, wsdlPort) + "\"/>";
      assertEquals(expectedSoapAddress, findSoapAddress(wsdl));
   }

   @Override
   protected void assertChangedConfiguration(String contextName) throws UnsupportedEncodingException, IOException, MalformedURLException
   {
      assertCorrectWsdlReturned(readFromUrlToString(createWsdlUrl(contextName)), contextName, WSDL_PORT_CHANGED);
      //if (testIfServiceIsFunctional())
      //  assertServiceIsFunctional(createServiceURL(contextName, WSDL_PORT_CHANGED)); will not work with wdl_port rewritten to new value
   }


   @Override
   protected void assertChangeConfigurationResult(CLIResult result)
   {
      //FIXME result.assertReloadRequired();
   }

   @Override
   protected void assertChangedConfigurationValue(CLIResult result)
   {
      result.assertResultAsStringEquals(WSDL_PORT_CHANGED + "");
   }

   @Override
   protected void assertUndefinedConfigurationValue(CLIResult result)
   {
      result.assertResultAsStringEquals(WSDL_PORT_UNDEFINED);
   }
}
