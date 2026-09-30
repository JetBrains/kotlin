// IGNORE_BACKEND_K2: ANY
// ISSUE: KT-89753

// FILE: Q.java

public class Q {
    public @interface A {}

    @A
    @lombok.Builder
    public static void f() {}
}

// FILE: test.kt

// Resolving `@A` on `Q.f()` while looking for `@Builder` must not re-enter the computation of `Q`'s nested classifiers.
fun box(): String {
    Q.f()
    return "OK"
}
