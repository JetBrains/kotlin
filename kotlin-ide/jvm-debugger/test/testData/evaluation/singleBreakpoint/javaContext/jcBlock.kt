// FILE: jcBlock.kt
package jcBlock

fun main(args: Array<String>) {
    val javaClass = forTests.javaContext.JavaClass()
    //Breakpoint!
    javaClass.block()
}

// STEP_INTO: 1
// STEP_OVER: 2

// EXPRESSION: bodyVal
// RESULT: 1: I

// EXPRESSION: thenVal
// RESULT: 1: I

// EXPRESSION: elseVal
// RESULT: Unresolved reference: elseVal

// FILE: forTests/javaContext/JavaClass.java
package forTests.javaContext;

public class JavaClass {
    public void block() {
        int bodyVal = 1;
        if (true) {
            int thenVal = 1;
            int breakpoint = 1;
        }
        else {
            int elseVal = 1;
        }
    }
}