annotation class ValueContainer

@ValueContainer
data class StringProperty(var v: String) {
    fun assign(v: String) {
        this.v = v
    }
    fun assign(v: StringProperty) {
        this.v = v.get()
    }
    fun get() = v
}

data class Task(var input: StringProperty)

fun `test local var reference and value`(): String {
    var property = StringProperty("OK")
    var originalProperty = property
    property = StringProperty("Fail")

    return when {
        originalProperty.get() != "OK" -> "Fail: ${originalProperty.get()}"
        originalProperty == property -> "Fail: originalProperty == property"
        else -> "OK"
    }
}

fun `test class property var reference and value`(): String {
    val task = Task(StringProperty("OK"))
    val originalProperty = task.input
    task.input = StringProperty("Fail")

    return when {
        originalProperty.get() != "OK" -> "Fail: ${originalProperty.get()}"
        originalProperty == task.input -> "Fail: originalProperty == task.input"
        else -> "OK"
    }
}

fun `test class property var reference and value with apply`(): String {
    val task = Task(StringProperty("OK"))
    val originalProperty = task.input
    task.apply {
        input = StringProperty("Fail")
    }

    return when {
        originalProperty.get() != "OK" -> "Fail: ${originalProperty.get()}"
        originalProperty == task.input -> "Fail: originalProperty == task.input"
        else -> "OK"
    }
}

fun box(): String {
    var result = `test local var reference and value`()
    if (result != "OK") return result

    result = `test class property var reference and value`()
    if (result != "OK") return result

    result = `test class property var reference and value with apply`()
    if (result != "OK") return result

    return "OK"
}
