annotation class a

annotation class b(val e: E)

enum class E { E1, E2 }

fun types(param: @[a] @[b(E.E1)] DoubleRange): @[a] @[b(E.E2)] Unit {}
