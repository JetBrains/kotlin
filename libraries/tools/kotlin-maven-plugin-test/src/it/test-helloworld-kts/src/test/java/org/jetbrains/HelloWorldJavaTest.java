package org.jetbrains;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelloWorldJavaTest {

    @Test
    public void greeting() {
        String[] args = {};
        org.jetbrains.HelloWorld hw = new org.jetbrains.HelloWorld(args);
        assertEquals("Hello, World!", hw.getGreeting());
    }
}
