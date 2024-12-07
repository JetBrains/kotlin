annotation class ValueContainer

@ValueContainer
class StringProperty(var v: String) {
    fun assign(v: String) {
        this.v = v
    }
    fun assign(v: StringProperty) {
        this.v = v.get()
    }
    fun get(): String {
        return v
    }
}

data class Task(val input: StringProperty)

var result = "Fail"
operator fun StringProperty.plusAssign(v: String) {
    result = v
}
operator fun StringProperty.plusAssign(v: StringProperty) {
    result = v.get()
}
operator fun StringProperty.set(i: Int, v: String) {
    result = v
}
operator fun StringProperty.set(i: Int, v: StringProperty) {
    result = v.get()
}
operator fun StringProperty.set(i: Int, j: Int, v: String) {
    result = v
}
operator fun StringProperty.set(i: Int, j: Int, v: StringProperty) {
    result = v.get()
}
operator fun StringProperty.set(i: Int, j: Int, k: Int, v: String) {
    result = v
}
operator fun StringProperty.set(i: Int, j: Int, k: Int, v: StringProperty) {
    result = v.get()
}
operator fun StringProperty.compareTo(v: String): Int {
    result = v
    return 0
}
operator fun StringProperty.compareTo(v: StringProperty): Int {
    result = v.get()
    return 0
}

fun box(): String {
    val task = Task(StringProperty("Fail"))

    // Double check that assign is correctly setup
    task.input = "OK"
    if (task.input.get() != "OK") return task.input.get()

    task?.input = "OK"
    if (task.input.get() != "OK") return task.input.get()

    result = "Fail"
    task.input += "OK"
    if (result != "OK") return result
    result = "Fail"
    task.input += StringProperty("OK")
    if (result != "OK") return result

    result = "Fail"
    task.input >= "OK"
    if (result != "OK") return result
    result = "Fail"
    task.input >= StringProperty("OK")
    if (result != "OK") return result

    result = "Fail"
    task.input <= "OK"
    if (result != "OK") return result
    result = "Fail"
    task.input <= StringProperty("OK")
    if (result != "OK") return result

    result = "Fail"
    task.input[0] = "OK"
    if (result != "OK") return result
    result = "Fail"
    task.input[0] = StringProperty("OK")
    if (result != "OK") return result

    result = "Fail"
    task.input[0, 0] = "OK"
    if (result != "OK") return result
    result = "Fail"
    task.input[0, 0] = StringProperty("OK")
    if (result != "OK") return result

    result = "Fail"
    task.input[0, 0, 0] = "OK"
    if (result != "OK") return result
    result = "Fail"
    task.input[0, 0, 0] = StringProperty("OK")
    if (result != "OK") return result

    return result
}
