// FIR_IDENTICAL
// ISSUE: KT-83085

// FILE: TestJavaClass.java

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestJavaClass {
    private static int age;
}

// FILE: TestJavaUsage.java 

public class TestJavaUsage {
    public static void main(String[] args) {
        TestJavaClass.getAge();     // error: cannot find symbol - correct
        TestJavaClass.setAge(1);    // error: cannot find symbol - correct
    }
}

// FILE: TestKotlinUsage.kt

fun foo(a: TestJavaClass) {
    TestJavaClass.getAge()      // It should be unresolved
    TestJavaClass.setAge(1)     // It should be unresolved

    a.<!UNRESOLVED_REFERENCE!>getAge<!>()
    a.<!UNRESOLVED_REFERENCE!>setAge<!>(1)
    a.<!UNRESOLVED_REFERENCE!>age<!>
}
