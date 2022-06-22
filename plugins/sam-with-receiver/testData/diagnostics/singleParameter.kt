// FILE: SamConstructor.kt
@SamWithReceiver
public interface Sam {
    void run(String a);
}

// FILE: test.kt
annotation class SamWithReceiver

fun test() {
    Sam <!ARGUMENT_TYPE_MISMATCH!>{ <!CANNOT_INFER_PARAMETER_TYPE!>a<!> ->
        System.out.<!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(a)
    }<!>

    Sam {
        val a: String = this
        val a2: String = <!UNRESOLVED_REFERENCE!>it<!>
        System.out.println(a)
    }
}
