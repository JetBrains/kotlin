// IGNORE_BACKEND_K1: ANY

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

fun box(): String {
    TestJava().use()
    val builder: Method.MethodBuilder<Any> = Method.builder()
    val builder2: Method.MethodBuilder<Any> = builder.m("s")
    val obj: Any = builder2.build()
    return "OK"
}