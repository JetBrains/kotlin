// FILE: rawTypeskt11831.kt
package rawTypeskt11831

fun main(args: Array<String>) {
    val raw = forTests.MyJavaClass.RawADerived()
    val foo = raw.foo(emptyList<String>())
    //Breakpoint!
    val a = foo
}

// EXPRESSION: foo
// RESULT: 1: I

// FILE: forTests/MyJavaClass.java
package forTests;

import org.jetbrains.annotations.NotNull;
import java.util.List;

public class MyJavaClass {
    public static class RawA<T> {
        public int foo(List<T> p) {
            return 1;
        }
    }

    public static class RawADerived extends RawA {

    }
}