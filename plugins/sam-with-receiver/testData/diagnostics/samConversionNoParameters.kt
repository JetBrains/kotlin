// FIR_IDENTICAL

// FILE: SamConstructor.kt
@SamWithReceiver
public interface Sam {
    void run();
}

// FILE: Exec.java
public class Exec {
    void exec(Sam sam) {}
}

// FILE: test.kt
annotation class SamWithReceiver

fun test() {
    val e = Exec()

    e.exec {
        System.out.println("Hello, world!")
    }

    e.exec {
        val a: String = <!NO_THIS!>this<!>
        System.out.println(a)
    }
}
