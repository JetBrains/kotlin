package foo

interface I {
    @Deprecated("")
    fun f(): Int

    @Deprecated("")
    var p: Int

    var i: Int
        @Deprecated("") set
}

interface II : I {
    override fun f(): Int {
    }
}

interface III : I {
    override fun f(): Int {
    }
}

class C : I {
    override fun f(): Int {
    }
}

interface IP : I {
    override var p: Int
        get() = throw UnsupportedOperationException()
        set(value) {
        }
}

interface IA : I {
    override var i: Int
        get() = throw UnsupportedOperationException()
        set(value) {
        }
}

interface Explicit : I {
    @Deprecated("a")
    override fun f(): Int {
    }
}

data class Pair(x: Int, y: Int)

class Exc {
    val (a, b) = Pair(1, 2)
}