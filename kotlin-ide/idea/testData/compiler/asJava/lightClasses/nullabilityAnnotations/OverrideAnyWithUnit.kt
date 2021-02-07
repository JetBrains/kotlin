// C

interface Base {
    fun foo(): Any
}

class C : Base {
    override fun foo(): Unit {}
}

// LAZINESS:NoLaziness