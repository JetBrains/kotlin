// WITH_STDLIB
// FILE: main.kt
package test

import javatest.JavaTest

annotation class NoArg

@NoArg 
class Test(val b: String)

fun box(): String {
    JavaTest.test()
    return "OK"
}

// FILE: javatest/JavaTest.java
package javatest;

import test.Test;

public class JavaTest {
    public static Test test() {
        // See KT-80633 for details
        return new Test();
    }
}
