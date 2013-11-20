package org.jboss.qa.management.ws.cli;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.qa.management.TestConstants;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Rule;
import org.junit.rules.Timeout;

//FIXME psakar inspect possibility to use CliClient and/or CLI.Result
public class CLITestUtils
{

   private static final String KEY_STARTUP_WAIT_MILLIS = "startupWaitMillis";
   private static final int DEFAULT_VALUE_STARTUP_WAIT_MILLIS = 1500;
   private static final String KEY_RELOAD_WAIT_MILLIS = "reloadWaitMillis";
   private static final int DEFAULT_VALUE_RELOAD_WAIT_MILLIS = 1000;
   private static final String KEY_SHUTDOWN_WAIT_MILLIS = "shutdownWaitMillis";
   private static final int DEFAULT_VALUE_SHUTDOWN_WAIT_MILLIS = 1500;
   private static final String KEY_TEST_TIMEOUT = "test.timeout";
   public static final String WAR_EXTENSTION = ".war";
   public static final String JAR_EXTENSTION = ".jar";
   public static final String EAR_EXTENSTION = ".ear";
   private final int shutdownWaitMillis;
   private final int reloadWaitMillis;
   private final int startupWaitMillis;

   // setting timeout for each test
   @Rule
   public Timeout timeout = new Timeout(readIntValueFromSystemProperties(KEY_TEST_TIMEOUT, TestConstants.SHORT_TEST_TIMEOUT));//FIXME remove * 1000 used for debug

   public CLITestUtils()
   {
     shutdownWaitMillis = readIntValueFromSystemProperties(KEY_SHUTDOWN_WAIT_MILLIS, DEFAULT_VALUE_SHUTDOWN_WAIT_MILLIS);
     reloadWaitMillis = readIntValueFromSystemProperties(KEY_RELOAD_WAIT_MILLIS, DEFAULT_VALUE_RELOAD_WAIT_MILLIS);
     startupWaitMillis = readIntValueFromSystemProperties(KEY_STARTUP_WAIT_MILLIS, DEFAULT_VALUE_STARTUP_WAIT_MILLIS);
   }

   static int readIntValueFromSystemProperties(String name, int defaultValue) {
     String value = System.getProperty(name);
     if (!(value == null || value.isEmpty())) {
       try {
         return Integer.parseInt(value);
       } catch (Exception e) {
         System.err.println("Can not parse int value from system property " + name + " with value " + value + " " + e.getMessage());
         e.printStackTrace(System.err);
       }
     }
     return defaultValue;
   }

   public void assertServiceIsNotAvailable(String serviceURL) throws MalformedURLException
   {
      QName serviceName = new QName("http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy", "AnnotatedSecurityService");
      URL wsdlURL = new URL(serviceURL + "?wsdl");
      try {
         Service service = Service.create(wsdlURL, serviceName);
         AnnotatedServiceIface proxy = service.getPort(AnnotatedServiceIface.class);
         proxy.sayHello();
         throw new IllegalStateException("Service " + serviceURL + " should not be accessible");
      } catch (WebServiceException e) {
         //expected
      }
   }

   public CLIResult executeAssertedCLICommand(String command) throws IOException, CommandLineException {
      return executeCLICommand(command).assertSuccess();
   }

   public void restartServer() throws IOException, CommandLineException {
      shutdownServer();
      startServer();
   }

   public void startServer() throws IOException
   {
      info("Start server");
      String startServerCommand = System.getProperty("jboss.start");
      if (startServerCommand == null) {
         String jbossHome = System.getenv("jboss.home");
         if (jbossHome == null)
            jbossHome = System.getProperty("jboss.home");
         if (jbossHome != null) {
            String extension = ".sh";
            startServerCommand = "/bin/sh -c " + FilenameUtils.normalizeNoEndSeparator(jbossHome) + File.separator + "bin" + File.separator + "standalone" + extension;
         }
      }
      if (startServerCommand == null)
         throw new IllegalStateException("Specify either java property jboss.start or jboss.home");
      info("Start server using command " + startServerCommand);
      Runtime.getRuntime().exec(startServerCommand);
      sleep(startupWaitMillis, "Start server");
   }

   public void shutdownServer() throws IOException, CommandLineException
   {
      info("Shutdown server");
      executeCLICommandQuietly("shutdown");
      sleep(shutdownWaitMillis, "Shutdown server");
      //FIXME psakar verify server is not running, if yes kill it
   }

   public void info(String message)
   {
      getLogger().info(message);
   }

   protected Logger getLogger()
   {
      return Logger.getLogger(getClass().getName());
   }

   public void error(String message, Exception e)
   {
      getLogger().log(Level.SEVERE, message, e);
   }

   void deployWar(WebArchive war) throws IOException, CommandLineException
   {
      executeCLIdeploy(war).assertSuccess();
   }

   protected String getContextName(WebArchive war)
   {
      return war.getName().replace(".war", "");
   }

   public CLIResult executeCLICommand(String command) throws IOException, CommandLineException
   {
      // Initialize the CLI context
      final CommandContext ctx;
      try
      {
         ctx = CommandContextFactory.getInstance().newCommandContext();
      }
      catch (CliInitializationException e)
      {
         throw new IllegalStateException("Failed to initialize CLI context", e);
      }

      try
      {
         // connect to the server controller
         ctx.connectController();
         //            ctx.connectController("http-remoting", "localhost", 9990); //TestSuiteEnvironment.getServerPort());
         //           ctx.connectController("localhost", 9990); //TestSuiteEnvironment.getServerPort());
         //ctx.connectController("http", "localhost", 9990); //TestSuiteEnvironment.getServerPort());

         info("Execute CLI command " + command);

         ModelNode request = ctx.buildRequest(command);
         ModelControllerClient client = ctx.getModelControllerClient();
         ModelNode result = client.execute(request);
         info("Result " + result.asString());
         return new CLIResult(result);
      }
      finally
      {
         ctx.terminateSession();
      }
   }

   public void assertUrlIsNotAccessible(URL url)
   {
      InputStream stream = null;
      try {
         stream  = url.openStream();
         throw new IllegalStateException("Url " + url.toString() + " should not be accessible");
      }
      catch (IOException e)
      {
         //expected
      } finally {
         IOUtils.closeQuietly(stream);
      }
   }

   public CLIResult executeCLIdeploy(Archive<?> archive) throws IOException, CommandLineException
   {
      String archiveName = archive.getName();
      assertArchiveNameContainsExtension(archiveName);
      File file = new File(FileUtils.getTempDirectory(), archiveName);
      archive.as(ZipExporter.class).exportTo(file, true);
      return executeCLICommand("deploy " + file.getAbsolutePath());
   }

   private void assertArchiveNameContainsExtension(String archiveName)
   {
      String extension = "." + FilenameUtils.getExtension(archiveName);
      if (!(WAR_EXTENSTION.equals(extension) || JAR_EXTENSTION.equals(extension) || EAR_EXTENSTION.equals(extension)))
         throw new IllegalArgumentException("Archive " + archiveName + " extension have to be either " + JAR_EXTENSTION + " or " + WAR_EXTENSTION + " or " + EAR_EXTENSTION);

   }

   public CLIResult executeCLICommandQuietly(String command) throws IOException, CommandLineException
   {
      try {
         return executeCLICommand(command);
      } catch (Exception e) {
         info(e.getMessage());
      }
      return null;
   }

   public CLIResult executeCLIUndeploy(String deploymentName) throws IOException, CommandLineException {
      return executeCLICommand("undeploy " + deploymentName);
   }

   public CLIResult undeployQuietly(String deploymentName)
   {
      try {
         return executeCLIUndeploy(deploymentName);
      } catch (Exception e) {
         // ignore
         getLogger().log(Level.WARNING, "Undeploy quietly error " + e.getMessage(), e);
      }
      return null;
   }


   public CLIResult executeCLIReload() throws IOException, CommandLineException
   {
      info("CLI Reload");
      //FIXME psakar find reliable way to find out server is reloaded //https://community.jboss.org/message/827388
      CLIResult result = executeCLICommand("reload").assertSuccess();
      //https://issues.jboss.org/browse/AS7-3561
      sleep(reloadWaitMillis, "CLI Reload");
      return result;
   }

   public void sleep(long millis, String name)
   {
      info("Waiting " + millis + " ms for " + name);
      try {
         Thread.sleep(reloadWaitMillis);
      } catch (InterruptedException e) {
         error("Interrupted " + e.getMessage(), e);
      }
   }

   //remove when //https://bugzilla.redhat.com/show_bug.cgi?id=996558
   protected void temporaryFixForBZ996558() throws IOException, CommandLineException {
      if (System.getProperty("BZ996558") != null)
         reloadServer();
   }

   protected void reloadServer() throws IOException, CommandLineException
   {
      executeCLIReload().assertSuccess();
   }


   public static final class CLIResult {
      public final ModelNode result;

      public CLIResult(ModelNode result)
      {
         this.result = result;
      }

      public CLIResult assertFailure() {
         assertFalse("Expected result failure, was " + result, result.asString().contains("\"outcome\" => \"success\""));
         return this;
      }

      public CLIResult assertSuccess() {
         assertTrue("Expected result success, was " + result, result.asString().contains("\"outcome\" => \"success\""));
         return this;
      }

      public CLIResult assertCLIOperationRequiesReload()
      {
         assertTrue(isReloadRequired());
         return this;
      }

      public boolean isReloadRequired()
      {
         return result.asString().contains("\"operation-requires-reload\" => true");
      }

      public CLIResult assertCLIResultIsReloadRequired()
      {
         assertTrue(result.asString().contains("\"process-state\" => \"reload-required\""));
         return this;
      }

      public CLIResult assertReloadRequired()
      {
         assertCLIOperationRequiesReload();
         assertCLIResultIsReloadRequired();
         return this;
      }
      public void assertResultAsStringEquals(String expected)
      {
         assertEquals(expected, result.get("result").asString());
      }
      public void assertIsUndefinedResult()
      {
         assertFalse("Expected undefined value, found " + result.asString(), result.get("result").isDefined());
      }

   }

   public static String readFromUrlToString(URL url) throws UnsupportedEncodingException, IOException
   {
      return readFromUrlToString(url, "UTF-8");
   }

   public static String readFromUrlToString(final URL url, String encoding) throws UnsupportedEncodingException, IOException
   {
      InputStream stream = null;
      InputStreamReader inputStream = null;
      URLConnection connection = null;
      HttpClient client = null;
      try {
        if (readIntValueFromSystemProperties("readUrlDirectly", 1) == 0) {
          client = new DefaultHttpClient();
          HttpGet get = new HttpGet(url.toURI());
          HttpResponse response = client.execute(get);
          return EntityUtils.toString(response.getEntity());
        } else {
         connection = url.openConnection();
         stream = connection.getInputStream();
         inputStream = new InputStreamReader(stream, encoding);
         return IOUtils.toString(inputStream);
        }
      } catch (URISyntaxException e) {
        throw new IllegalStateException("Can not read from " + url.toString() + " " + e.getMessage(), e);
      } catch (ConnectException e) {
        throw new IllegalStateException("Can not read from " + url.toString() + " " + e.getMessage(), e);
      } catch (IOException e) {

        throw new IllegalStateException("Error reading from " + url.toString() + " " + e.getMessage(), e);
      } finally {
        IOUtils.close(connection); //has to be closed first, otherwise the connection is pooled and reused !!!!
        IOUtils.closeQuietly(inputStream);
        IOUtils.closeQuietly(stream);
        if (client != null)
          client.getConnectionManager().shutdown();
      }
   }

   protected String findFirstLineContaining(String searchedString, String content)
   {
      List<String> lines = convertToLines(content);
      for (String line : lines)
      {
         if (line.contains(searchedString))
            return line;
      }
      return null;
   }

   protected List<String> convertToLines(String content)
   {
      if (content == null)
         return new ArrayList<String>();
      return Arrays.asList(content.split("\n"));
   }

   protected static final String SOAP_ADDRESS_LOCATION_PREFIX = "<soap:address location=\"";

   protected String findSoapAddress(String wsdl)
   {
      return StringUtils.trim(findFirstLineContaining(SOAP_ADDRESS_LOCATION_PREFIX, wsdl));
   }

}
