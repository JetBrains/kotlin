annotation class ValueContainer

@ValueContainer
class Container(private var storage: String) {
    fun assign(value: String) {
        storage = value
    }

    fun assign(container: Container) {
        storage = container.storage
    }

    operator fun plus(value: String): Container {
        return Container(storage + value)
    }
}

val property = Container("foo")

fun test() {
    <expr>property += "bar"</expr>
}
