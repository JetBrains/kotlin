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
    return testJava.<!UNRESOLVED_REFERENCE!>canEqual<!>(1)
}

class KotlinChild : TestJava() {
    <!NOTHING_TO_OVERRIDE!>override<!> fun canEqual(other: Any?): Boolean {
        return super.<!UNRESOLVED_REFERENCE!>canEqual<!>(other)
    }
}

fun box(): String {
    return if (usage(TestJava())) "FAIL" else "OK"
}
