package testing.rename

interface AP {
    val first: Int // <--- Rename base here
}

public open class BP: AP {
    override val first: Int get() = 12 // <-- Rename with Java getter here
}

class CP: BP() {
    override val first = 2 // <--- Rename overriden here
}

class CPOther {
    val first: Int = 111
}

fun usagesProp() {
    val b = BP()
    val a: AP = b
    val c = CP()

    a.first
    b.first
    c.first

    CPOther().first
}