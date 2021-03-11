package inlineFunInClassInitializer

fun main() {
    Goo()
}

class Foo(val a: String?)

class DumbList<T>(private val element: T) : AbstractList<T>() {
    override val size get() = 1
    override fun get(index: Int) = if (index == 0) element else error("Invalid index $index")
}

class Goo {
    init {
        //Breakpoint!
        val a = DumbList(Foo("a"))
        val b = a.mapNotNull { it.a }
        1
    }
}

// STEP_OVER: 2