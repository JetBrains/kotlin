// CORRECT_ERROR_TYPES

// FILE: kapt/StaticMethod.java
package kapt;

public class StaticMethod<T> {
    public static <T> StaticMethod<T> of(T t1) {
        return new StaticMethod<T>(t1);
    }
    public static <T> StaticMethod<T> of(T t1, T t2) {
        return new StaticMethod<T>(t1, t2);
    }

    public static <T> StaticMethod<T> of2(T t1) {
        return new StaticMethod<T>(t1);
    }

    private final T[] ts;

    private StaticMethod(T... ts) {
        this.ts = ts;
    }
}

// FILE: lib.kt
package my.lib

fun String.func(): Int = this.length

// FILE: test.kt
package kapt

import java.util.Collections.unmodifiableCollection
import kapt.StaticMethod.of
import kapt.StaticMethod.of2
import my.lib.func

class StaticImport {
    val x = unmodifiableCollection(listOf(""))
    val l = of("hello", "world")
    val m = of2("hello")
    val y = "1".func()
}