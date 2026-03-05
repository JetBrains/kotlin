package org.jetbrains;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelloWorldJavaTest {

    @Test
    public void greeting() {
        assertEquals("Hello, World!", org.jetbrains.HelloWorldKt.getGreeting());
    }
}
