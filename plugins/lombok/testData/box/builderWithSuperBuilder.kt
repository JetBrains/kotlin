// IGNORE_BACKEND_K2: ANY
// ISSUE: KT-83352

// FILE: TestJava.java
import lombok.Builder;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Builder
public class TestJava  {
    int a;
}

// FILE: TestJavaUsage.java
public class TestJavaUsage {
    public static void main(String[] args) {
        TestJava test = TestJava.builder().build();   //OK
    }
}

// FILE: test.kt
fun box(): String {
    TestJava.builder().build()
    return "OK"
}