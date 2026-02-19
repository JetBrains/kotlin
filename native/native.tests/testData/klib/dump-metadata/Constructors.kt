// FIR_IDENTICAL
@file:Suppress("UNUSED_PARAMETER")

annotation class A

class Foo(x: Int) {
    constructor(): this(0)
    constructor(x: Double): this(x.toInt())
    constructor(x: Double, y: Int): this(y)

    private constructor(x: Long): this(0)
    protected constructor(x: String): this(0)
    @A constructor(x: Foo) : this(0)
}

class Bar @A constructor(x: Int)
class Baz private constructor(x: Int)
class Qux protected constructor(x: Int)

class Typed<T>(x: Int) {
    constructor(): this(0)
    constructor(x: Double): this(x.toInt())
    constructor(x: Double, y: Int): this(y)

    private constructor(x: Long): this(0)
    protected constructor(x: String): this(0)
    @A constructor(x: Foo) : this(0)
}
