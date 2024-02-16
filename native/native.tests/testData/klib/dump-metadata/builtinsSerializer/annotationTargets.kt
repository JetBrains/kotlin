// FIR_IDENTICAL
package test

annotation class anno(val x: String)


@anno("top level function")
fun f1(@anno("top level function parameter") p: Int) {}

@anno("top level property")
val p1 = null

@anno("extension function")
fun Long.f2(@anno("extension function parameter") p: Int) {}

@anno("extension property")
val Double.p2: Double get() = 0.0

@anno("top level class")
class C1 @anno("constructor") constructor() {
    @anno("member function")
    fun f3(@anno("member function parameter") p: Int) {}

    @anno("member property")
    val p3 = null

    @anno("member extension function")
    fun String.f4() {}

    @anno("member extension property")
    val Int.v4: Int get() = this

    @anno("nested class")
    class C2

    @anno("companion object")
    companion object {}
}
