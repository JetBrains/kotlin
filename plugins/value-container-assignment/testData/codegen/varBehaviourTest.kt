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

fun box(): String {
    run {
        // Local var should not be affected by the plugin and reference should be replaced
        var property = StringProperty("OK")
        var originalProperty = property
        property = StringProperty("Fail")
        if (originalProperty.get() != "OK") return "Fail: ${originalProperty.get()}"
        if (originalProperty == property) return "Fail: originalProperty == property"
    }

    run {
        // Class property var should not be affected by the plugin and reference should be replaced
        val task = Task(StringProperty("OK"))
        val originalProperty = task.input
        task.input = StringProperty("Fail")
        if (originalProperty.get() != "OK") return "Fail: ${originalProperty.get()}"
        if (originalProperty == task.input) return "Fail: originalProperty == task.input"
    }

    run {
        // Class property var should not be affected by the plugin with apply and reference should be replaced
        val task = Task(StringProperty("OK"))
        val originalProperty = task.input
        task.apply {
            input = StringProperty("Fail")
        }
        if (originalProperty.get() != "OK") return "Fail: ${originalProperty.get()}"
        if (originalProperty == task.input) return "Fail: originalProperty == task.input"
    }
    return "OK"
}
