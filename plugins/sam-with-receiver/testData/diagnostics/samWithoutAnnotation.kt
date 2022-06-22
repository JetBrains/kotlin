// FILE: SamConstructor.kt
public interface Sam {
    String run(String a, String b);
}

// FILE: test.kt
fun test() {
    Sam { a, b ->
        System.out.println(a)
        ""
    }

    Sam <!ARGUMENT_TYPE_MISMATCH!>{ b ->
        val a = this<!UNRESOLVED_LABEL!>@Sam<!>
        System.out.<!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(a)
        ""
    }<!>
}
