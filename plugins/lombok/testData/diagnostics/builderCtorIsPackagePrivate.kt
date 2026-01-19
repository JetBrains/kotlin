// FIR_IDENTICAL
// ISSUE: KT-83336

// FILE: test/TestJava.java
package test;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TestJava {
    String name;
    int age;
}

// FILE: TestJavaUsage.java
public class TestJavaUsage {
    public static void main(String[] args) {
        TestJava test = new TestJava("name", 1); // error: TestJava(String,int) is not public in TestJava;
    }
}

// FILE: test.kt
fun main() {
    test.TestJava("name", 1) // It should be incorrect
}