// !DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER,-UNUSED_VARIABLE

// FILE: Sam.java
public interface Sam {
    String run(String a, String b);
}

// FILE: test.kt
fun test() {
    Sam { a, b ->
        System.out.println(a)
        ""
    }

    Sam { <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>b<!> ->
        val a = <!NO_THIS!>this@Sam<!>
        System.out.println(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>)
        ""
    }
}