// RUN_PIPELINE_TILL: FRONTEND
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
    JavaEntity.builder().<!UNRESOLVED_REFERENCE!>staticField<!>(2)
    return "OK"
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, javaFunction, stringLiteral */
