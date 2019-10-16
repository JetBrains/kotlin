package org.jetbrains;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class HelloWorldJavaTest {

    @Test
    public void greeting() {
        String[] args = {};
        org.jetbrains.HelloWorld hw = new org.jetbrains.HelloWorld(args);
        assertEquals("Hello, World!", hw.getGreeting());
    }
}
