// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-83251

// FILE: TestJava.java
import lombok.Value;

@Value
public class TestJava  {
    int a;
    String b = "init";
}

// FILE: TestJavaUsage.java

public class TestJavaUsage {
    public static void main(String[] args) {
        TestJava testJava = new TestJava(1);    //OK
        testJava.getB();                        //OK
    }
}

// FILE: TestKotlinUsage.kt

fun box(): String {
    val test = <!NONE_APPLICABLE!>TestJava<!>(1)  // It should be OK
    test.<!UNRESOLVED_REFERENCE!>b<!>             // It should be OK

    return "OK"
}
