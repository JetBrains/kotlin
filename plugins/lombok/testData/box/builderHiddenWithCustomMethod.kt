// ISSUE: KT-83329
// FILE: TestJava.java

import lombok.Builder;

@Builder(builderMethodName = "internalBuilder")
public class TestJava {
    public int a;

    public static TestJavaBuilder builder(int a) {
        return internalBuilder().a(a);
    }
}

// FILE: test.kt

fun box(): String {
    val obj: TestJava = TestJava.builder(1).build()
    return if (obj.a == 1) "OK" else "FAIL: a=${obj.a}"
}
