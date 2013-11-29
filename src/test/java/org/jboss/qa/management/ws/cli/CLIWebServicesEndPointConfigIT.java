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

import static org.jboss.qa.management.ws.cli.AnnotatedServiceWithTestEndpointConfigImpl.*;
import static org.jboss.qa.management.ws.cli.LogicalSourceHandler.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.cli.CommandLineException;
import org.jboss.qa.management.ws.BaseDeployment.WarDeployment;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class CLIWebServicesEndPointConfigIT extends CLITestUtils {

  static final String NAME = "CLIWebServicesEndPointConfigIT";

  static final String NAME2 = "CLIWebServicesEndPointConfigIT2";

  private final WebArchive war;

  private final WebArchive anotherWar;

  public CLIWebServicesEndPointConfigIT() {
    this.war = createWarDeployment(NAME + WAR_EXTENSTION).createArchive();
    this.anotherWar = createWarDeploymentWithWrongEnpointConfig(NAME2 + WAR_EXTENSTION).createArchive();
  }

  static WarDeployment createWarDeployment(String name) {
    return new WarDeployment(name) {
      {
        archive
            .setManifest(
                new StringAsset("Manifest-Version: 1.0\n" + "Dependencies: org.jboss.ws.cxf.jbossws-cxf-client\n"))
            .addClass(org.jboss.qa.management.ws.cli.AnnotatedServiceWithTestEndpointConfigIface.class)
            .addClass(org.jboss.qa.management.ws.cli.AnnotatedServiceWithTestEndpointConfigImpl.class)
            .addClass(LogicalSourceHandler.class);
      }
    };
  }

  static WarDeployment createWarDeploymentWithWrongEnpointConfig(String name) {
    return new WarDeployment(name) {
      {
        archive
            .setManifest(
                new StringAsset("Manifest-Version: 1.0\n" + "Dependencies: org.jboss.ws.cxf.jbossws-cxf-client\n"))
            .addClass(org.jboss.qa.management.ws.cli.AnnotatedServiceWithTestEndpointConfigIface.class)
            .addClass(org.jboss.qa.management.ws.cli.AnnotatedServiceWithUnexistingEndpointConfigImpl.class)
            .addClass(LogicalSourceHandler.class);
      }
    };
  }

  @After
  public void after() throws Exception {
    info("After");
    undeployQuietly(war.getName());
    undeployQuietly(anotherWar.getName());
    resetConfiguration();
    reloadServer();
  }

  protected void resetConfiguration() throws IOException, CommandLineException {
    String endpointConfingName = Constants.ENDPOINT_CONFIG_NAME;
    String resetConfigurationCommand = "/subsystem=webservices/endpoint-config=" + endpointConfingName + "/:remove";
    executeCLICommandQuietly(resetConfigurationCommand);
  }

  @Test
  public void testDeployOfWebServiceRequiringPredefinedEndpointConfigWillFailWhenPredefinedEndpointConfigIsNotDefined()
      throws Exception {
    addEndpointConfigAndReloadServer();
    executeCLIdeploy(anotherWar).assertFailure(" see https://bugzilla.redhat.com/show_bug.cgi?id=1029762");
  }

  @Test
  public void testDeployOfWebServiceRequiringPredefinedEndpointConfigSucceedsWhenPredefinedEndpointConfigIsDefined()
      throws Exception {
    addEndpointConfigAndReloadServer();
    executeCLIdeploy(war).assertSuccess();
  }

  @Test
  public void testPredefinedEndpointConfigIsApplied() throws Exception {
    addEndpointConfigAndReloadServer();
    deployWar(war);
    assertEndpointConfigApplied();
  }

  @Test
  public void testPredefinedEndpointConfigIsAppliedAfterConfigurationChangeBeforeServerReload() throws Exception {
    addEndpointConfigAndReloadServer();
    deployWar(war);
    removeEndpointConfig();
    assertEndpointConfigApplied();
  }


  @Test
  public void testPredefinedEndpointConfigIsAppliedAfterConfigurationChangeBeforeServerReloadForNewDeployment() throws Exception {
    addEndpointConfigAndReloadServer();
    removeEndpointConfig();
    deployWar(war);
    assertEndpointConfigApplied();
  }

  private void assertEndpointConfigApplied() throws Exception {
    assertServiceIsFunctional(createServiceURL(NAME));
  }

  private String createServiceURL(String contextName) {
    return createServiceURL("localhost", contextName, CLIWebservicesWsdlPortIT.PORT); ///*JBossWSTestHelper.getServerHost()*/
  }

  private String createServiceURL(String wsdlHost, String contextName, int port) {
    return "http://" + wsdlHost + ":" + port + "/" + contextName + "/"
        + Constants.SERVICE_NAME_ENDPOINT_CONFIG_TEST;
  }

  protected void assertServiceIsFunctional(String serviceURL) throws MalformedURLException {
    AnnotatedServiceWithTestEndpointConfigIface proxy = createServiceProxy(serviceURL);
    String message = HELLO_WORLD;
    assertEquals(message + INBOUND_APPENDIX + OUTBOUND_APPENDIX, proxy.say(message));
  }

  protected AnnotatedServiceWithTestEndpointConfigIface createServiceProxy(String serviceURL)
      throws MalformedURLException {
    QName serviceName = new QName(Constants.NAMESPACE, Constants.SERVICE_NAME_ENDPOINT_CONFIG_TEST);
    URL wsdlURL = new URL(serviceURL + "?wsdl");
    Service service = Service.create(wsdlURL, serviceName);
    return service.getPort(AnnotatedServiceWithTestEndpointConfigIface.class);
  }

  private void addEndpointConfigAndReloadServer() throws Exception {
    addEndpointConfig();
    reloadServer();
  }

  private void addEndpointConfig() throws Exception {
    String endpointConfingName = Constants.ENDPOINT_CONFIG_NAME;
    String createEndpointConfigCLIcommand = "/subsystem=webservices/endpoint-config=" + endpointConfingName + "/:add";
    executeAssertedCLICommand(createEndpointConfigCLIcommand);
    String handlerChainName = "test-handlers";
    String handlerChainProtocols = "##SOAP11_HTTP ##SOAP11_HTTP_MTOM ##SOAP12_HTTP ##SOAP12_HTTP_MTOM";
    String addPostHandlerChainCLIcommand = "/subsystem=webservices/endpoint-config=" + endpointConfingName + "/post-handler-chain=" + handlerChainName + ":add(protocol-bindings=\"" + handlerChainProtocols + "\")";
    executeAssertedCLICommand(addPostHandlerChainCLIcommand);
    String handlerName = "test-handler";
    String handlerClass = LogicalSourceHandler.class.getName();
    String addHandlerToChainCLIcommand = "/subsystem=webservices/endpoint-config=" + endpointConfingName + "/post-handler-chain=" + handlerChainName + "/handler=" + handlerName + ":add(class=\"" + handlerClass + "\")";
    executeAssertedCLICommand(addHandlerToChainCLIcommand);
  }

  protected void removeEndpointConfig() throws IOException, CommandLineException {
    String endpointConfingName = Constants.ENDPOINT_CONFIG_NAME;
    String command = "/subsystem=webservices/endpoint-config=" + endpointConfingName + "/:remove";
    executeCLICommand(command).assertSuccess();
  }

}
