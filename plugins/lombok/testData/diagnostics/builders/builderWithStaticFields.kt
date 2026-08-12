// ISSUE: KT-88505
// FILE: JavaEntity.java

import lombok.Builder;

@Builder
public class JavaEntity {
    private int instance;
    private static int staticField = 1;
}

// FILE: main.kt

fun box(): String {
    // Resolves and compiles, but `staticField` is not a real builder method.
    // It should be a compile error (unresolved reference).
    JavaEntity.builder().staticField(2)
    return "OK"
}
