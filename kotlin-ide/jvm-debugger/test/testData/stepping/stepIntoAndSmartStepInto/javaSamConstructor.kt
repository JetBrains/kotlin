// FILE: javaSamConstructor.kt
package javaSamConstructor

import forTests.MyJavaClass

fun main(args: Array<String>) {
    //Breakpoint!
    MyJavaClass { /* do nothing*/ }
}

// FILE: forTests/MyJavaClass.java
package forTests;

public class MyJavaClass {
    public MyJavaClass() {}
    public MyJavaClass(Runnable runnable) {}
}