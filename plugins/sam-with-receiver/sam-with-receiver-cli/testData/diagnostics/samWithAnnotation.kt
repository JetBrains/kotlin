// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE

// FILE: Sam.java
@SamWithReceiver
public interface Sam {
    String run(String a, String b);
}

// FILE: test.kt
annotation class SamWithReceiver

fun test() {
    Sam { <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>a, <!CANNOT_INFER_PARAMETER_TYPE!>b<!><!> ->
        System.out.println(a)
        ""
    }

    Sam { b ->
        val a: String = this
        System.out.println(a)
        ""
    }
}