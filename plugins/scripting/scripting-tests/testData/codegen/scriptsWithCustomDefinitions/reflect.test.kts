// WITH_REFLECT

// example from the issue KT-68685
import kotlin.reflect.full.memberProperties

data class Response(
    val list: List<Data>,
) {
    data class Data(
        val id: String,
    )
}

val mp1 = Response::class.memberProperties.single()
val mp2 = Response.Data::class.memberProperties.single()

// simplified and extended repro
class O {
    class K {
        inner class `!`
        companion object {
            fun foo(): String {
                class Foo
                return Foo::class.simpleName!!
            }
        }
    }
}

// checks metadata FQNs of nested declarations
val o = O::class.simpleName
val k = O.K::class.simpleName
val `!` = O.K.`!`::class.simpleName

fun bar(): String {
    class Bar
    return Bar::class.simpleName!! // checks metadata FQNs of local classes
}

fun baz(): O.K = O.K()

val bazRet = ::baz.returnType.classifier?.toString()?.substringAfter("class ") // checks correct metadata FQNs of types

val rv = "$o$k$`!`_${O.K.foo()}_${bar()}_$bazRet"

// expected: rv: OK!_Foo_Bar_Reflect_test$O$K
