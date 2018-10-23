// EXPECTED_REACHABLE_NODES: 1196
object Foo {
    val value = "O"
}
class Bar(val anotherValue: String)

val Foo.prop: String
    get() = value

val Bar.prop: String
    get() = anotherValue

fun box() = Foo.prop + Bar("K").prop