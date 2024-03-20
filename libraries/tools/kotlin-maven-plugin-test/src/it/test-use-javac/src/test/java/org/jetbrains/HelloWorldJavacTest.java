package org.jetbrains;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class HelloWorldJavacTest {

    @Test
    public void greeting() {
        assertEquals("Hello, Javac World!", org.jetbrains.HelloWorldKt.getGreeting());
    }
}
