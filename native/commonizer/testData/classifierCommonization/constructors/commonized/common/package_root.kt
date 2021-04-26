expect class A1(text: String) {
    constructor(number: Int)
}

expect class A2(text: String) {
    constructor(number: Int)
}

expect class A3
expect class A4
expect class A5

expect class B1 protected constructor(text: String) {
    protected constructor(number: Int)
}

expect class B2
expect class B3

expect class C1 internal constructor(text: String) {
    internal constructor(number: Int)
}

expect class C2

expect class D1

expect class E {
    constructor(a: Any)

    constructor(a: Any, b: String)
    constructor(a: Any, b: Int)
}

expect enum class F {
    FOO,
    BAR,
    BAZ;

    // no constructor allowed for enum class
    val alias: String
}
