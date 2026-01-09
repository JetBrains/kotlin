// FIR_IDENTICAL
// ISSUE: KT-83217

// FILE: TestJava.java

import lombok.With;

public class TestJava {
    @With static String staticField;

    public TestJava(String a) {
        staticField = a;
    }
}

// FILE: TestJavaUsage.java

public class TestJavaUsage {
    public static void main(String[] args) {
        TestJava testJava = new TestJava("a");
        TestJava testJava2 = testJava.withStaticField("");
    }
}

// FILE: UsageFromKotlin.kt

fun main() {
    TestJava("a").withStaticField("") // It shouldn't be OK
}
