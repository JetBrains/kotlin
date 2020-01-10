// WITH_RUNTIME
package jvmField

class Foo {
    @JvmField
    var x: String = ""
}

object Bar {
    @JvmField
    val y: Int = 0
}

@JvmField
val q: Array<String>? = null

class User(@JvmField val firstName: String)