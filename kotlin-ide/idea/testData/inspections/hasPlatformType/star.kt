// WITH_RUNTIME

class Wrapper<T>(val value: T)

fun star(): Wrapper<Wrapper<*>> = Wrapper(Wrapper(Any()))

fun star2(): List<*> = listOf(Any())