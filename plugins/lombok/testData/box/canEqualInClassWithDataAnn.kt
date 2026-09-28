// ISSUE: KT-83119

// FILE: TestJava.java

import lombok.Data;

@Data
public class TestJava {
    private String name;
}

// FILE: TestJavaUsage.java

public class TestJavaUsage {
    public static void main(String[] args) {
        TestJava testJava = new TestJava();
        testJava.canEqual(1);                 //OK
    }
}

// FILE: test.kt

import kotlin.test.assertFalse

fun usage(testJava: TestJava): Boolean {
    return testJava.canEqual(1)
}

class KotlinChild : TestJava() {
    override fun canEqual(other: Any?): Boolean {
        return super.canEqual(other)
    }
}

fun box(): String {
    assertFalse(usage(TestJava()))

    return "OK"
}
