// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-83330
// JDK_KIND: FULL_JDK_17
// WITH_STDLIB

// FILE: TestJava.java
import lombok.Builder;
import lombok.Singular;

import java.util.List;

@Builder
public record TestJava(@Singular List<String> jobs) { }

// FILE: TestJavaUsage.java
import java.util.Arrays;

public class TestJavaUsage {
    public static void test() {
        TestJava.builder().jobs(Arrays.asList("A", "B")).build();   //OK
        TestJava.builder().job("A").build();                        //OK
    }
}

// FILE: test.kt
fun box(): String {
    TestJavaUsage.test()
    TestJava.builder().jobs(mutableListOf("A", "B")).build()
    TestJava.builder().job("A").build()
    return "OK"
}
