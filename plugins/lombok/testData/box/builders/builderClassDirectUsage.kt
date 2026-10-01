// ISSUE: KT-83273

// FILE: TestJava.java
import lombok.Builder;

@Builder
public class TestJava  {
    String a;
}

// FILE: test.kt
fun box(): String {
    val builder = TestJava.TestJavaBuilder()
    val test = builder.a("OK").build()
    return test.a
}
