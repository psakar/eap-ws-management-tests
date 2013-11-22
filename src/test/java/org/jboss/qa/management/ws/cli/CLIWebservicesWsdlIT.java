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

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.management.ws.BaseDeployment.WarDeployment;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public final class CLIWebservicesWsdlIT extends CLITestUtils
{
   private static final int WSDL_PORT_PRECONFIGURED = PORT;

   static final String NAME = "CLIWebservicesWsdlIT";

   final WebArchive war;

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
       }
     };
   }

   public CLIWebservicesWsdlIT() {
     war = createWarDeployment(NAME + WAR_EXTENSTION).createArchive();
     info("system property http.keepAlive is " + System.getProperty("http.keepAlive"));
   }

   @After
   public void after () {
     undeployQuietly(war.getName());
   }

   @Test
   public void testWsdlIsAccessibleAfterReload() throws Exception {
     deployWar(war);
     String contextName = getContextName(war);
     assertOriginalConfiguration(contextName);
     int reloadCount = readIntValueFromSystemProperties("testWsdlIsAccessibleAfterReload.reloadCount", 3);
     for (int i = 0; i < reloadCount; i++) {
       info("Reloading server - pass " + (i+1));
       reloadServer();
       info("Reloaded");
       assertOriginalConfiguration(contextName);
       info("Asserted");
     }
   }


   private static String createResourcePrefix()
   {
      return CLIWebservicesModifyWsdlAddressIT.class.getName().toLowerCase().replace(".", "/") + "/";
   }

   private URL createWsdlUrl(String name) throws MalformedURLException
   {
      String random = (readIntValueFromSystemProperties("appendRandomPameterToWsdlUrl", 0) == 0 ? "" : "&time=" + System.currentTimeMillis());
      return new URL(createServiceURL(name) + "?wsdl" + random);
   }


   private String createServiceURL(String contextName)
   {
      return createServiceURL(contextName, WSDL_PORT_PRECONFIGURED);
   }

   private String createServiceURL(String contextName, int port)
   {
      return createServiceURL("localhost", contextName, port); ///*JBossWSTestHelper.getServerHost()*/
   }

   private String createServiceURL(String wsdlHost, String contextName, int port)
   {
      return "http://" + wsdlHost + ":" + port + "/" + contextName + "/AnnotatedSecurityService";
   }


   private void assertOriginalConfiguration(String contextName) throws UnsupportedEncodingException, IOException, MalformedURLException, InterruptedException
   {
      String wsdlContent = readFromUrlToString(createWsdlUrl(contextName));
      assertCorrectWsdlReturned(wsdlContent, contextName, PORT);
      // assertServiceIsFunctional(createServiceURL(contextName, WSDL_PORT_CHANGED)); will not work with wdl_port rewritten to wrong value
   }

   private void assertCorrectWsdlReturned(String wsdl, String contextName, int wsdlPort)
   {
      assertTrue("Expected wsdl contains sayHelloResponse, found following wsdl: '" + wsdl + "'", wsdl.contains("sayHelloResponse"));
      String expectedSoapAddress = SOAP_ADDRESS_LOCATION_PREFIX + createServiceURL(CLIWebservicesWsdlHostIT.WSDL_HOST, contextName, wsdlPort) + "\"/>";
      assertEquals(expectedSoapAddress, findSoapAddress(wsdl));
   }

}
