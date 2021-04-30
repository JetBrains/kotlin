// !IGNORE_FIR

interface Base {
    fun print()
}

class BaseImpl(val x: Int) : Base {
    override fun print() {
        print(x)
    }
}

class Derived(b: Base) : Base by createBase(10), CharSequence by "abc"

fun createBase(i: Int): Base {
    return BaseImpl(i)
}
