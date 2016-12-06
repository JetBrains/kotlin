// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE

// FILE: Sam.java
@SamWithReceiver
public interface Sam {
    void run(String a);
}

// FILE: Exec.java
public class Exec {
    void exec(Sam sam) {}
}

// FILE: test.kt
annotation class SamWithReceiver

fun test() {
    val e = Exec()

    e.exec <!TYPE_MISMATCH!>{ <!CANNOT_INFER_PARAMETER_TYPE!>a<!> -> System.out.println(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>) }<!>
    e.exec { System.out.println(this) }
}