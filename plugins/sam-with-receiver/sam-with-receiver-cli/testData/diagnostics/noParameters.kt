// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE

// FILE: Sam.java
@SamWithReceiver
public interface Sam {
    void run();
}

// FILE: test.kt
annotation class SamWithReceiver

fun test() {
    Sam {
        System.out.println("Hello, world!")
    }

    Sam {
        val a: String = <!NO_THIS!>this<!>
        System.out.println(a)
    }
}