// FILE: test.kt
annotation class ValueContainer

typealias VC = ValueContainer

@VC
interface KotlinProperty<T> {
    fun assign(argument: T);
    fun get(): T;
}

data class KotlinStringProperty(private var v: String): KotlinProperty<String> {
    override fun assign(v: String) {
        this.v = v
    }
    override fun get() = this.v
}

@VC
data class KotlinClassStringProperty(private var v: String) {
    fun assign(v: String) {
        this.v = v
    }
    fun get(): String {
        return v
    }
}

fun `should work with annotation on Kotlin interface`(): String {
    data class Task(val input: KotlinStringProperty)
    val task = Task(KotlinStringProperty("Fail"))
    task.input = "OK"

    return if (task.input.get() != "OK") {
        "Fail: ${task.input.get()}"
    } else {
        "OK"
    }
}

fun `should work with annotation on Kotlin class`(): String {
    data class Task(val input: KotlinClassStringProperty)
    val task = Task(KotlinClassStringProperty("Fail"))
    task.input = "OK"

    return if (task.input.get() != "OK") {
        "Fail: ${task.input.get()}"
    } else {
        "OK"
    }
}

fun box(): String {
    var result = `should work with annotation on Kotlin interface`()
    if (result != "OK") return result

    result = `should work with annotation on Kotlin class`()
    if (result != "OK") return result

    return "OK"
}
