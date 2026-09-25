// ISSUE: KT-83273
// FIR_DUMP

// FILE: TestJava.java
import lombok.Builder;

@Builder
public class TestJava  {
    int a;
}

// FILE: test.kt
fun usage() {
    val builder = TestJava.<!UNRESOLVED_REFERENCE!>TestJavaBuilder<!>()
    builder.a(1).build()
    val justBuilder: TestJava.TestJavaBuilder? = null
    justBuilder?.a(2)?.build()
}
