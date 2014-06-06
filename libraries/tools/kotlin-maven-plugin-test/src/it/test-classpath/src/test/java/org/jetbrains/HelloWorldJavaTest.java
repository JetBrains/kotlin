package org.jetbrains;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class HelloWorldJavaTest {

    @Test
    public void testClasspath() {
        assertEquals("OK", org.jetbrains.JetbrainsPackage.box());
    }
}
