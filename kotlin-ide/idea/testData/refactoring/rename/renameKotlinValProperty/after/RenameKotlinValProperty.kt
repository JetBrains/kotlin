package testing.rename

interface AP {
    val second: Int // <--- Rename base here
}

public open class BP: AP {
    override val second: Int get() = 12 // <-- Rename with Java getter here
}

class CP: BP() {
    override val second = 2 // <--- Rename overriden here
}

class CPOther {
    val first: Int = 111
}

fun usagesProp() {
    val b = BP()
    val a: AP = b
    val c = CP()

    a.second
    b.second
    c.second

    CPOther().first
}