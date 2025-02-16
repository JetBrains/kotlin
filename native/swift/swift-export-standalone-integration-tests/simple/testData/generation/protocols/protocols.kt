// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

object MyObject

interface Foeble {
    fun bar(arg: Int): Int
    val baz: Int
}

interface Barable: Foeble

interface Bazzable

class Bar: Barable, Foeble, Bazzable {
    override fun bar(arg: Int): Int {
        TODO("Not yet implemented")
    }

    override val baz: Int
        get() = TODO("Not yet implemented")
}

// FILE: less_trivial.kt

interface OUTSIDE_PROTO {
    // FIXME: KT-70541
    // We can not properly detect nested classes as unsopported
    /*
   open class INSIDE_PROTO
    */
}

// FIXME: See the commend above on OUTSIDE_PROTO.INSIDE_PROTO
/*
    class INHERITANCE_COUPLE : OUTSIDE_PROTO.INSIDE_PROTO(), OUTSIDE_PROTO
    class INHERITANCE_SINGLE_PROTO : OUTSIDE_PROTO.INSIDE_PROTO()
*/

object OBJECT_WITH_INTERFACE_INHERITANCE: OUTSIDE_PROTO

enum class ENUM_WITH_INTERFACE_INHERITANCE: OUTSIDE_PROTO

// FILE: existentials.kt

fun normal(value: Foeble): Foeble = value
var normal: Foeble = Bar()
fun nullable(value: Foeble?): Foeble? = value
var nullable: Foeble? = null