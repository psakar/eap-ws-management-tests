package org.jboss.qa.management;

/**
 * @author rhatlapa (rhatlapa@redhat.com)
 */
public class TestConstants {
    public static final int DEFAULT_TEST_TIMEOUT = 5*60*1000;   // five minutes, the timeout is mainly meant for cases where CLI starts hanging for not yet known reasons, see https://issues.jboss.org/browse/WFLY-1964?_sscc=t
    public static final int SHORT_TEST_TIMEOUT = 30*1000;   // 30 seconds
}
