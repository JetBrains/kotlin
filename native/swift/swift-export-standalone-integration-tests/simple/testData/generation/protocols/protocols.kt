// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

object MyObject

interface Foeble {
    fun bar(arg: Foeble): Foeble
    val baz: Foeble
}

interface Barable: Foeble {
    override fun bar(arg: Foeble): Barable
    override val baz: Foeble
}

interface Bazzable

class Foo: Foeble {
    override fun bar(arg: Foeble): Foo = this
    override val baz: Foeble get() = this
}

class Bar: Barable, Foeble, Bazzable {
    override fun bar(arg: Foeble): Bar = this
    override val baz: Bar get() = this
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
fun list(value: List<Foeble>): List<Foeble> = value
var list: List<Foeble> = emptyList()