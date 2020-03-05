actual class A1 actual constructor(text: String) { actual constructor(number: Int) : this(number.toString()) }
actual class A2 actual constructor(text: String) { actual constructor(number: Int) : this(number.toString()) }
actual class A3 protected constructor(text: String) { protected constructor(number: Int) : this(number.toString()) }
actual class A4 internal constructor(text: String) { internal constructor(number: Int) : this(number.toString()) }
actual class A5 private constructor(text: String) { private constructor(number: Int) : this(number.toString()) }

actual class B1 protected actual constructor(text: String) { protected actual constructor(number: Int) : this(number.toString()) }
actual class B2 internal constructor(text: String) { internal constructor(number: Int) : this(number.toString()) }
actual class B3 private constructor(text: String) { private constructor(number: Int) : this(number.toString()) }

actual class C1 internal actual constructor(text: String) { internal actual constructor(number: Int) : this(number.toString()) }
actual class C2 private constructor(text: String) { private constructor(number: Int) : this(number.toString()) }

actual class D1 private constructor(text: String) { private constructor(number: Int) : this(number.toString()) }

actual class E {
    constructor(a: String)
    actual constructor(a: Any)

    constructor(a: String, b: Int)
    constructor(a: String, b: Any)
    actual constructor(a: Any, b: String)
    actual constructor(a: Any, b: Int)
}

actual enum class F(actual val alias: String) {
    FOO("foo"),
    BAR("bar"),
    BAZ("baz")
}
