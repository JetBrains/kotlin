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

import kotlin.test.assertEquals

fun box(): String {
    val viaStaticConstructor = A.of(1, "1")
    assertEquals(A(1, "1"), viaStaticConstructor)
    return "OK"
}
