// SNIPPET

enum class E {
    FIRST,
    SECOND
}

val res1 = E.values().toList().toString()

// EXPECTED: res1 == [FIRST, SECOND]

// SNIPPET

// no artificial classes are generated for simple enum entries
val res2 = E.FIRST.javaClass == E.SECOND.javaClass

// EXPECTED: res2 == true
