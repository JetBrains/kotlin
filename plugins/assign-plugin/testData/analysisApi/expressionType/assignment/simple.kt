annotation class ValueContainer

@ValueContainer
class Container(private var storage: String) {
    fun assign(value: String) {
        storage = value
    }
}

val property = Container("foo")

fun test() {
    <expr>property = "bar"</expr>
}
