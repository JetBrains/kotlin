package test

interface ValueProvider {
    fun value(): Int
}

inline fun inlineValue(transform: (Int) -> Int): Int {
    val provider = object : ValueProvider {
        override fun value(): Int = 21
    }
    return transform(provider.value())
}
