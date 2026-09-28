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

// SNIPPET

// KT-15407: the function body is resolved lazily, from the use below, and still sees the enum from the previous snippet
fun applySomething(build: E) = when (build) {
    E.FIRST -> "OK"
    E.SECOND -> "fail"
}

fun isFirst(e: E) = if (e == E.FIRST) "OK" else "fail"

val res3 = applySomething(E.FIRST) + isFirst(E.FIRST)

// EXPECTED: res3 == OKOK
