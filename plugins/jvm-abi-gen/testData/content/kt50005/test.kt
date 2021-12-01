package test

// The order of fields and methods is visible to annotation processors
// and thus part of the public ABI of a library.
class A {
    @JvmField
    val b: Int = 0
    lateinit var a: String

    fun g() {}
    fun f() {}
    fun h() {}
}

