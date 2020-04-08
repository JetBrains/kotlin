open class Foo {}
open class Bar {}

fun <T : Bar, T1> foo(<warning>x</warning> : Int) {}
fun <T1, T : Foo> foo(<warning>x</warning> : Long) {}

fun f(): Unit {
    foo<<error>Int</error>, Int>(1)
}