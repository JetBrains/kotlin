// IGNORE_BACKEND_K1: ANY
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

fun usage(testJava: TestJava): Boolean {
    return testJava.canEqual(1)
}

class KotlinChild : TestJava() {
    override fun canEqual(other: Any?): Boolean {
        return super.canEqual(other)
    }
}

fun box(): String {
    return if (usage(TestJava())) "FAIL" else "OK"
}
