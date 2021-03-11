// FILE: javaStaticMethods.kt
package javaStaticMethods

import forTests.javaContext.JavaClass.JavaStatic

fun main() {
    //Breakpoint!
    val a = 5
}

// EXPRESSION: JavaStatic.state()
// RESULT: 1: I

// FILE: forTests/javaContext/JavaClass.java
package forTests.javaContext;

public class JavaClass {
    public interface JavaStatic {
        static int state() { return 1; }
    }
}