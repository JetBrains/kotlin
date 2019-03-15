// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE

// FILE: Sam.java
@SamWithReceiver
public interface Sam {
    void run(String a);
}

// FILE: test.kt
annotation class SamWithReceiver

fun test() {
    Sam { <!CANNOT_INFER_PARAMETER_TYPE, EXPECTED_PARAMETERS_NUMBER_MISMATCH!>a<!> ->
        System.out.println(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>)
    }

    Sam {
        val a: String = this
        val a2: String = <!UNRESOLVED_REFERENCE!>it<!>
        System.out.println(a)
    }
}