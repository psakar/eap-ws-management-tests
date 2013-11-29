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

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.cli.CommandLineException;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * see https://docspace.corp.redhat.com/docs/DOC-152480
 *
 */

@RunWith(Arquillian.class)
@RunAsClient
public abstract class CLITestCase extends CLITestUtils
{

   final WebArchive war;
   final WebArchive anotherWar;

   private final String verifyConfigurationCommand;
   private final String changeConfigurationCommand;
   private final String resetConfigurationCommand;
   private final String undefineConfigurationCommand;
   private final String[] invalidValueConfigurationCommands;

   public CLITestCase(String verifyConfigurationCommand, String changeConfigurationCommand, String resetConfigurationCommand, String undefineConfigurationCommand, String [] invalidValueConfigurationCommands, WebArchive war, WebArchive anotherWar)
   {
      this.verifyConfigurationCommand = verifyConfigurationCommand;
      this.changeConfigurationCommand = changeConfigurationCommand;
      this.resetConfigurationCommand = resetConfigurationCommand;
      this.undefineConfigurationCommand = undefineConfigurationCommand;
      this.invalidValueConfigurationCommands = invalidValueConfigurationCommands;
      this.war = war;
      this.anotherWar = anotherWar;
   }

   @After
   public void after() throws Exception {
      info("After");
      undeployQuietly(war.getName());
      undeployQuietly(anotherWar.getName());
      resetConfiguration();
      reloadServer();
   }

   protected void resetConfiguration() throws IOException, CommandLineException
   {
      executeCLICommandQuietly(resetConfigurationCommand);
   }

   @Test
   public void testDefaultConfiguration() throws Exception
   {
      deployWar(war);

      assertDefaultConfigurationValue(executeCLICommand(verifyConfigurationCommand).assertSuccess());

      assertOriginalConfiguration(getContextName(war));
   }

   @Test//1BA
   public void testChangeRequiringReloadDoesNotAffectNewDeploymentBeforeReload() throws Exception
   {
      changeConfiguration();

      deployWar(war);

      assertOriginalConfiguration(getContextName(war));
   }

   @Test//2BA
   public void testChangeFollowedByReloadAffectsNewDeployments() throws Exception
   {
      changeConfiguration();
      reloadServer();

      deployWar(war);

      assertChangedConfiguration(getContextName(war));
   }

   @Test//3BA
   public void testChangeRequiringReloeadDoesNotAffectExistingDeploymentsBeforeReload() throws Exception
   {
      deployWar(war);

      changeConfiguration();

      assertOriginalConfiguration(getContextName(war));

   }

   @Test//4BA
   public void testChangeAffectsExistingDeploymentsAfterReload() throws Exception
   {
      deployWar(war);

      changeConfiguration();

      reloadServer();

      assertChangedConfiguration(getContextName(war));
   }

   @Test//5BA
   public void testChangeRequiringReloadAffectsNewDeploymentsAndExistingDeploymentsAfterReload() throws Exception
   {
      deployWar(war);

      changeConfiguration();

      deployWar(anotherWar);

      assertOriginalConfiguration(getContextName(war));
      assertOriginalConfiguration(getContextName(anotherWar));

      reloadServer();

      assertChangedConfiguration(getContextName(war));
      assertChangedConfiguration(getContextName(anotherWar));
   }


   @Test
   public void testUndefinedConfigurationIsAppliedAfterReload() throws Exception
   {
      prepareServerWithChangedConfiguration();

      changeConfigurationUndefine();

      deployWar(war);

      reloadServer();

      assertUndefinedConfiguration(getContextName(war));
   }

   @Test//1BA
   public void testChangeToUndefinedDoesNotAffectNewDeploymentsWithoutReload() throws Exception
   {
      prepareServerWithChangedConfiguration();

      changeConfigurationUndefine();

      deployWar(war);

      assertChangedConfiguration(getContextName(war));
   }

   @Test//2BA
   public void testChangeToUndefinedFollowedByReloadAffectsNewDeployments() throws Exception
   {
      prepareServerWithChangedConfiguration();

      changeConfigurationUndefine();
      reloadServer();

      deployWar(war);

      assertUndefinedConfiguration(getContextName(war));
   }

   @Test//3BA
   public void testChangeToUndefinedDoesNotAffectExistingDeploymentsBeforeReload() throws Exception
   {
      prepareServerWithChangedConfiguration();

      deployWar(war);

      changeConfigurationUndefine();

      assertChangedConfiguration(getContextName(war));

   }

   @Test//4BA
   public void testChangeToUndefinedAffectsExistingDeploymentsAfterReload() throws Exception
   {
      prepareServerWithChangedConfiguration();
      deployWar(war);

      changeConfigurationUndefine();
      reloadServer();

      assertUndefinedConfiguration(getContextName(war));
   }

   @Test//5BA
   public void testChangeToUndefinedAffectsNewDeploymentsAndExistingDeploymentsAfterReload() throws Exception
   {
      prepareServerWithChangedConfiguration();
      deployWar(war);

      changeConfigurationUndefine();

      deployWar(anotherWar);

      assertChangedConfiguration(getContextName(war));
      assertChangedConfiguration(getContextName(anotherWar));

      reloadServer();

      assertUndefinedConfiguration(getContextName(war));
      assertUndefinedConfiguration(getContextName(anotherWar));
   }


   @Test//1V
   public void testSetInvalidValueResultsInError() throws Exception
   {
      for (String invalidValueConfigurationCommand : invalidValueConfigurationCommands)
      {
         executeCLICommand(invalidValueConfigurationCommand).assertFailure();
      }
   }

   protected abstract void assertDefaultConfigurationValue(CLIResult result);

   protected abstract void assertOriginalConfiguration(String contextName) throws UnsupportedEncodingException, IOException, MalformedURLException, InterruptedException;

   protected abstract void assertUndefinedConfiguration(String contextName) throws UnsupportedEncodingException, IOException, MalformedURLException;

   protected abstract void assertChangedConfiguration(String contextName) throws UnsupportedEncodingException, IOException, MalformedURLException;

   protected abstract void assertUndefinedConfigurationValue(CLIResult result);

   protected abstract void assertChangedConfigurationValue(CLIResult result);

   protected abstract void assertChangeConfigurationResult(CLIResult result);


   protected void prepareServerWithChangedConfiguration() throws IOException, CommandLineException
   {
      changeConfiguration();
      reloadServer();
   }

   private void changeConfiguration() throws IOException, CommandLineException
   {
      assertChangeConfigurationResult(executeCLICommand(changeConfigurationCommand).assertSuccess());
      assertChangedConfigurationValue(executeCLICommand(verifyConfigurationCommand).assertSuccess());
   }

   private void changeConfigurationUndefine() throws IOException, CommandLineException
   {
      assertChangeConfigurationResult(executeCLICommand(undefineConfigurationCommand).assertSuccess());
      assertUndefinedConfigurationValue(executeCLICommand(verifyConfigurationCommand).assertSuccess());
   }

   protected void assertServiceIsFunctional(String serviceURL) throws MalformedURLException
   {
      AnnotatedServiceIface proxy = createServiceProxy(serviceURL);
      assertEquals(AnnotatedServiceImpl.HELLO_WORLD, proxy.sayHello());
   }

   protected AnnotatedServiceIface createServiceProxy(String serviceURL) throws MalformedURLException
   {
      QName serviceName = new QName(Constants.NAMESPACE, "AnnotatedSecurityService");
      URL wsdlURL = new URL(serviceURL + "?wsdl");
      Service service = Service.create(wsdlURL, serviceName);
      return service.getPort(AnnotatedServiceIface.class);
   }

  protected boolean testIfServiceIsFunctional() {
      return readIntValueFromSystemProperties("testIfServiceIsFunctional", 0) == 1;
   }



}
