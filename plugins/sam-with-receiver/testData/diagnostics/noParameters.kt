// FIR_IDENTICAL

// FILE: SamConstructor.kt
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
