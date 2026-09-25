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

// FILE: testWithImport.kt
import TestJava.TestJavaBuilder

fun usageWithImport() {
    val builder = <!RESOLUTION_TO_CLASSIFIER!>TestJavaBuilder<!>()
    builder.<!UNRESOLVED_REFERENCE!>a<!>(1).build()
    val justBuilder: TestJavaBuilder? = null
    justBuilder?.a(2)?.build()
}

// FILE: testWithStarImport.kt
import TestJava.*

fun usageWithStarImport() {
    val builder = <!RESOLUTION_TO_CLASSIFIER!>TestJavaBuilder<!>()
    builder.<!UNRESOLVED_REFERENCE!>a<!>(1).build()
    val justBuilder: TestJavaBuilder? = null
    justBuilder?.a(2)?.build()
}
