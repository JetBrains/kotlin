package org.jetbrains;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * User: danielpenkin
 * Date: 06.05.12
 * Time: 23:35
 */
public class HelloWorldJavaTest {

    @Test
    public void greeting() {
        assertEquals("Hello, World!", org.jetbrains.JetbrainsPackage.getGreeting());
    }
}
