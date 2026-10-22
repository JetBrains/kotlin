// JDK_KIND: FULL_JDK_21
// MIN_JAVA_VERSION: 16

annotation class A

interface I {
    fun foo(): Int
}

@A
@JvmRecord
data class R<T>(@A val x: Int, val y: T): I {
    constructor(y: T) : this(0, y)

    override fun foo(): Int  = 0
}

@Target(AnnotationTarget.FIELD)
annotation class F

@JvmRecord
data class WithCompanion(@field:F val s: String?, val list: List<String>, val array: IntArray) {
    @JvmRecord
    data class Nested(val i: Long)

    companion object {
        const val C = 1
    }
}

@JvmRecord
data class PrivateConstructor private constructor(val i: Int)

@JvmRecord
data class Narrow(val b: Byte, val s: Short, val c: Char, val name: String) {
    constructor(name: String) : this(1, 2, 'c', name)
}

@JvmRecord
data class Throwing @Throws(java.io.IOException::class) constructor(@get:Throws(java.io.IOException::class) val i: Int) {
    @Throws(java.io.IOException::class)
    constructor() : this(0)

    @Throws(java.io.IOException::class)
    fun i(x: Int): Int = x
}

@JvmRecord
data class PrivateComponent(private val x: Int, val y: Int) {
    constructor() : this(0, 0)
}
