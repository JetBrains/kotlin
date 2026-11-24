// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-91284
// FILE: A.java

import lombok.Data;

@Data(staticConstructor = "of")
public class A {
    private final int a;
    private final String b;

    public A(int a, String b) {
        this.a = a + 2;
        this.b = b + "_suffix";
    }

    static void javaUsage() {
        A direct = new A(1, "1");
        A viaStaticConstructor = A.of(1, "1");
    }
}

// FILE: test.kt

fun foo() {
    val viaStaticConstructor = A.of(1, "1")
    val direct = <!INVISIBLE_REFERENCE!>A<!>(1, "1")
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, javaFunction, javaType, localProperty, propertyDeclaration,
stringLiteral */
