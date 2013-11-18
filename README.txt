Testsuite for testing management operations of EAP server
=========================================================
Usage
-----
to run all test
  mvn clean integration-test

to run single test
  mvn clean integration-test -Dit.test=testCaseToRun


Package structure
-----------------
All packages start with org.jboss.qa.management (in short o.j.q.m)


Packages in src/main/java
o.j.q.m.cli - wrappers and abstraction for CLI operations
o.j.q.m.jmx - wrappers and abstraction for JMX operations
o.j.q.m.native - abstraction for native administration operations (direct change in config files, ...)
o.j.q.m.common - common utils, such as utils for accessing URL, file system operations, ...

Packages in src/test/java
o.j.q.m.${subsystem}
package contains only tests which can not leave server with changed configuration after running/failure
 - test cases are named *IT.java
 - test cases are using arquillian in managed mode (server start/stop is managed by arquillian)

o.j.q.m.${subsystem}.manual
package contains tests which are dangerous and could crash the whole server configuration or leave the server with changed configuration after running/failure.
 - test cases are named *ITManual.java
 - test cases are using arquillian in manual mode (mode where it is possible to manage server start/stop in the test, ...)
 - test cases should use backup/restore server configuration (for example see hornetQ testsuite where are already used some tools working with arquillian
which handles backup/restore of server configuration)


Maven related notes
-------------------
For integration testing we use maven-failsafe-plugin which runs in integration-test phase of maven lifecycle - currently
as test frameworks are used both JUnit and TestNG with reports stored separately

The EAP is automatically downloaded from brew maven repository and unpacked to build directory (target)

We don't use resource filtering

To use as external dependency in your testsuite (if you cannot add your tests to this project but you want to use it as library) add following dependencies to pom.xml:
        <dependency>
            <groupId>org.jboss.as</groupId>
            <artifactId>jbossqe-eap-tests-management</artifactId>
            <version>1.0-SNAPSHOT</version>
            <type>jar</type>
        </dependency>
        <dependency>
            <groupId>org.jboss.as</groupId>
            <artifactId>jbossqe-eap-tests-management</artifactId>
            <version>1.0-SNAPSHOT</version>
            <type>test-jar</type>
        </dependency>

To redeploy this artifact run
  mvn clean deploy -DskipTests


Code formating
--------------
import files from FIXME
Idea - please use http://plugins.jetbrains.com/plugin/?id=6546


Eclipse related
---------------
To run tests from Eclipse system property jboss.home must be defined and contain path to your AS installation
(eg -Djboss.home=/home/development/jboss-eap-6.1)

To allow to connect to running server user system property -DallowConnectingToRunningServer=true

Add it either to the debug configuration to Arguments | VM Arguments or to JRE (Window -> Preferences -> Java -> Installled JREs, edit selected and add it to 'Default VM Arguments')
