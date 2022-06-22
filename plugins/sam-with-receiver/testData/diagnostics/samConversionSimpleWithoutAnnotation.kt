// FILE: SamConstructor.kt
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
    e.exec { System.out.<!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(<!NO_THIS!>this<!>) }
}
