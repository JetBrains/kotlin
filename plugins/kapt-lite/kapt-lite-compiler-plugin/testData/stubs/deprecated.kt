// WITH_RUNTIME
package deprecated

@Deprecated("Foo is deprecated")
class Foo @Deprecated("constructor is deprecated") public constructor(@Deprecated("n1 is deprecated") val n1: Int) {

    @Deprecated("secondary constructor is deprecated")
    constructor() : this(0)

    @Deprecated("foo() is deprecated")
    fun foo(a: Int, b: String) {}

    @Deprecated("x is deprecated")
    val x: String = ""

    var y: Int
        @Deprecated("y/get is deprecated") get() = 1
        @Deprecated("y/set is deprecated") set(newValue) {}

    @Deprecated("Nested is deprecated")
    class Nested {
        @Deprecated("p() is deprecated")
        fun p() {}
    }
}

@Deprecated("EnumClass is deprecated")
enum class EnumClass {
    FOO, @Deprecated("BAR is deprecated") BAR;

    @Deprecated("goo is deprecated")
    fun goo() {}
}

fun String.ext() {}

@Deprecated("topLevel() is deprecated")
fun topLevel() {}

@Deprecated("Intf is deprecated")
interface Intf

@Deprecated("Obj is deprecated")
object Obj {
    @JvmField
    @Deprecated("f is deprecated")
    val f: Int = 0

    @Deprecated("c is deprecated")
    const val c: Int = 5
}

@Deprecated("Anno is deprecated")
annotation class Anno(@Deprecated("value is deprecated") val value: String)