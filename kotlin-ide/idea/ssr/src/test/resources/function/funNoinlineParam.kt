inline fun <T> myInline(body: () -> T): T = body()

<warning descr="SSR"><warning descr="[NOTHING_TO_INLINE] Expected performance impact from inlining is insignificant. Inlining works best for functions with parameters of functional types">inline</warning> fun <T> myInlineTwo(noinline body: () -> T): T = body()</warning>

inline fun <T> myInlineThree(crossinline body: () -> T): T = body()