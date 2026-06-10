// COMPILER_PLUGIN: kotlin-assignment-compiler-plugin-2.4.20.jar annotation=ValueContainer

annotation class ValueContainer

@ValueContainer
class StringProperty(var value: String) {
    fun assign(newValue: String) {
        value = newValue
    }
}

class Container {
    val input = StringProperty("")
}

fun box(): String {
    val container = Container()
    container.input = "OK"
    return container.input.value
}
