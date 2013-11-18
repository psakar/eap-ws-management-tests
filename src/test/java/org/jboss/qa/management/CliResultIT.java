package org.jboss.qa.management;

import junit.framework.Assert;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.cli.scriptsupport.CLI;
import org.jboss.dmr.ModelNode;
import org.junit.*;
import org.junit.rules.*;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;

/**
 * Just a small smoke test for CLI tool
 *
 * @author Petr Kremensky <pkremens@redhat,com>
 */
@RunWith(Arquillian.class)
public class CliResultIT {
    private static CLI cli;
    private static final String command = ":read-resource()";
    private static final String failCommand = ":foo is bar ()";
    private static CLI.Result result;

    // setting timeout for each test
    @Rule
    public Timeout timeout = new Timeout(TestConstants.DEFAULT_TEST_TIMEOUT);

    @BeforeClass
    public static void connect() {
        cli = CLI.newInstance();
        cli.connect();
        result = cli.cmd(command);
    }

    @AfterClass
    public static void disconnect() {
        cli.disconnect();
    }

    @Test
    public void commandEquals() {
        Assert.assertEquals(command, result.getCliCommand());
    }

    @Test
    public void compareResponse() {
        ModelNode response = result.getResponse();
        String outcome = response.get(OUTCOME).asString();
        Assert.assertTrue("Outcome of '" + command + "' should be success.", outcome.equals("success"));
    }

    @Test
    public void isSuccess() {
        Assert.assertTrue("Calling of '" + command + "' wasn't successful.", result.isSuccess());
    }

    @Test
    public void testFailure() {
        CLI.Result result = cli.cmd(failCommand);
        Assert.assertFalse("Command \'" + failCommand + "\' shouldn't success.", result.isSuccess());
    }

    @Test
    public void compareFailedResponse() {
        CLI.Result result = cli.cmd(failCommand);
        ModelNode response = result.getResponse();
        String outcome = response.get(OUTCOME).asString();
        Assert.assertTrue("Outcome of '" + command + "' shouldn't be success.", outcome.equals("failed"));
    }
}
