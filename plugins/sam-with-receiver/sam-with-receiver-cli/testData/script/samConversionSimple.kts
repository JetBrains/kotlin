// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE,-UNUSED_ANONYMOUS_PARAMETER

// FILE: SamWithReceiver1.java
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SamWithReceiver1 {
}

// FILE: Sam.java
@SamWithReceiver1
public interface Sam {
    void run(String a, String b);
}

// FILE: Exec.java
public class Exec {
    void exec(Sam sam) { sam.run("a", "b") }
}

// FILE: test.kts
val e = Exec()

e.exec <!TYPE_MISMATCH!>{ <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>a, <!CANNOT_INFER_PARAMETER_TYPE!>b<!><!> -> System.out.println(a) }<!>
e.exec { b ->
    val a: String = this
    System.out.println(a)
}
