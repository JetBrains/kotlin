class A1(text: String) {
    constructor(number: Int) : this(number.toString())
}

class A2 constructor(text: String) {
    constructor(number: Int) : this(number.toString())
}

class A3 protected constructor(text: String) {
    protected constructor(number: Int) : this(number.toString())
}

class A4 internal constructor(text: String) {
    internal constructor(number: Int) : this(number.toString())
}

class A5 private constructor(text: String) {
    private constructor(number: Int) : this(number.toString())
}

class B1 protected constructor(text: String) {
    protected constructor(number: Int) : this(number.toString())
}

class B2 internal constructor(text: String) {
    internal constructor(number: Int) : this(number.toString())
}

class B3 private constructor(text: String) {
    private constructor(number: Int) : this(number.toString())
}

class C1 internal constructor(text: String) {
    internal constructor(number: Int) : this(number.toString())
}

class C2 private constructor(text: String) {
    private constructor(number: Int) : this(number.toString())
}

class D1 private constructor(text: String) {
    private constructor(number: Int) : this(number.toString())
}

class E {
    constructor(a: String)
    constructor(a: Any)

    constructor(a: String, b: Int)
    constructor(a: String, b: Any)
    constructor(a: Any, b: String)
    constructor(a: Any, b: Int)
}

enum class F(val alias: String) {
    FOO("foo"),
    BAR("bar"),
    BAZ("baz")
}
