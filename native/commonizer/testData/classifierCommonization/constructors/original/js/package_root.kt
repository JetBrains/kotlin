class A1(text: String) {
    constructor(number: Int) : this(number.toString())
}

class A2(text: String) {
    constructor(number: Int) : this(number.toString())
}

class A3(text: String) {
    constructor(number: Int) : this(number.toString())
}

class A4(text: String) {
    constructor(number: Int) : this(number.toString())
}

class A5(text: String) {
    constructor(number: Int) : this(number.toString())
}

class B1 protected constructor(text: String) {
    protected constructor(number: Int) : this(number.toString())
}

class B2 protected constructor(text: String) {
    protected constructor(number: Int) : this(number.toString())
}

class B3 protected constructor(text: String) {
    protected constructor(number: Int) : this(number.toString())
}

class C1 internal constructor(text: String) {
    internal constructor(number: Int) : this(number.toString())
}

class C2 internal constructor(text: String) {
    internal constructor(number: Int) : this(number.toString())
}

class D1 private constructor(text: String) {
    private constructor(number: Int) : this(number.toString())
}

class E {
    constructor(a: Int)
    constructor(a: Any)

    constructor(a: Int, b: String)
    constructor(a: Int, b: Any)
    constructor(a: Any, b: String)
    constructor(a: Any, b: Int)
}

enum class F(val alias: String) {
    FOO("foo"),
    BAR("bar"),
    BAZ("baz")
}
