internal class A() {

    constructor(a: Int) : this() {}

    protected constructor(c: Char) : this() {}

    constructor(f: Float) : this() {}

    private constructor(d: Double) : this() {}
}

class B() {

    constructor(a: Int) : this() {}

    protected constructor(c: Char) : this() {}

    internal constructor(f: Float) : this() {}

    private constructor(d: Double) : this() {}
}