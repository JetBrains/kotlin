// FILE: kotlinSamFunction.kt
package kotlinSamFunction

import forTests.MyJavaClass

class KotlinSubclass : MyJavaClass() {
    override fun other(runnable: Runnable) {
        super.other(runnable)
    }
}

fun main(args: Array<String>) {
    val klass = KotlinSubclass()
    //Breakpoint!
    klass.other { /* do nothing*/ }
}

// FILE: forTests/MyJavaClass.java
package forTests;

public class MyJavaClass {
    public void other(Runnable runnable) {
        runnable.run();
    }
}
