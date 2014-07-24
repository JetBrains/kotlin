package org.jetbrains;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class HelloWorldJavaTest {

    @Test
    public void greeting() {
        assertEquals("Hello, World!", org.jetbrains.JetbrainsPackage.getGreeting());
    }
}
