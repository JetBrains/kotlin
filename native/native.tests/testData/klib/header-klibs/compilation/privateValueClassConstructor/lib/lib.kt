package lib

value class A private constructor(val value: String) {
    companion object { fun a() = A("OK") }
    inline fun b() = value
}
