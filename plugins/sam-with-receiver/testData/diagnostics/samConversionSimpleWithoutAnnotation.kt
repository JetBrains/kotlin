// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE

// FILE: Sam.java
public interface Sam {
    void run(String a);
}

// FILE: Exec.java
public class Exec {
    void exec(Sam sam) {}
}

// FILE: test.kt
fun test() {
    val e = Exec()

    e.exec { a -> System.out.println(a) }
    e.exec { System.out.println(<!NO_THIS!>this<!>) }
}