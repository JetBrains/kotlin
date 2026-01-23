// FIR_IDENTICAL
// ISSUE: KT-83334
// Check incorrect using of `@Builder` and its robust handling in Kotlin
// TODO: probably it makes sense to add more examples.
// For instance, a constructor with its own type parameter (it's illegal in Lombok)

// FILE: Method.java

import lombok.Builder;

public class Method {
    @Builder(builderClassName = "MethodBuilder")
    static <M> M method(M m) {
        return m;
    }
}

// FILE: TestJava.java

public class TestJava {
    void use() {
        // It seems a legal code in Java
        Method.MethodBuilder<Object> builder = Method.builder();
        Method.MethodBuilder<Object> builder2 = builder.m("s");
        Object obj = builder2.build();
    }
}

// FILE: testKotlin.kt

fun test() {
    val builder: Method.MethodBuilder<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any><!> = Method.builder()
    val builder2: Method.MethodBuilder<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any><!> = builder.<!UNRESOLVED_REFERENCE!>m<!>("s")
    val obj: Any = builder2.<!UNRESOLVED_REFERENCE!>build<!>()
}
