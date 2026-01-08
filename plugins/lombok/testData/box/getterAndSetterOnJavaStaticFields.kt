// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-83078

// FILE: TestJavaClass.java

import lombok.Getter;
import lombok.Setter;

public class TestJavaClass {
    @Getter
    @Setter
    private static String name = "";
}

// FILE: TestJavaUsage.java

public class TestJavaUsage {
    public static void main(String[] args) {
        TestJavaClass.getName();
        TestJavaClass.setName("");
    }
}

// FILE: UsageFromKotlin.kt

fun box(): String {
    TestJavaClass.<!UNRESOLVED_REFERENCE!>getName<!>()
    TestJavaClass.<!UNRESOLVED_REFERENCE!>setName<!>("")
    return "OK"
}
