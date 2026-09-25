// ISSUE: KT-83329
// FIR_DUMP
// FILE: TestJava.java
import lombok.Builder;

@Builder(builderMethodName = "internalBuilder")
class TestJava {
    int a;

    public static TestJavaBuilder builder(int a) {
        return internalBuilder().a(1);
    }
}


// FILE: test.kt
fun foo() {
    val internalBuilder = TestJava.internalBuilder()
    val builder = TestJava.<!MISSING_DEPENDENCY_CLASS!>builder<!>(1)
}
