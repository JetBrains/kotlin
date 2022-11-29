// FILE: Property.java
@ValueContainer
public interface Property<T> {
    void set(T v);
    T get();
}

// FILE: JavaTaskWithField.java
public class JavaTaskWithField {
    public final StringProperty input;

    public JavaTaskWithField(StringProperty input) {
        this.input = input;
    }
}

// FILE: JavaTaskWithProperty.java
public class JavaTaskWithProperty {
    private final StringProperty input;

    public JavaTaskWithProperty(StringProperty input) {
        this.input = input;
    }

    public StringProperty getInput() {
        return input;
    }
}

// FILE: test.kt
annotation class ValueContainer

data class StringProperty(var v: String): Property<String> {
    override fun set(v: String) {
        this.v = v
    }
    fun assign(v: String) {
        this.v = v
    }
    fun assign(v: Property<String>) {
        this.v = v.get()
    }
    override fun get(): String {
        return v
    }
}

data class Task(val input: StringProperty)

fun `should work with assignment for raw type`(): String {
    val task = Task(StringProperty("Fail"))
    task.input = "OK"

    return if (task.input.get() != "OK") {
        "Fail: ${task.input.get()}"
    } else {
        "OK"
    }
}

fun `should work with assignment for wrapped type`(): String {
    val task = Task(StringProperty("Fail"))
    task.input = StringProperty("OK")

    return if (task.input.get() != "OK") {
        "Fail: ${task.input.get()}"
    } else {
        "OK"
    }
}

fun `should work with assignment with apply for raw type`(): String {
    val task = Task(StringProperty("Fail"))
    task.apply {
        input = "OK"
    }

    return if (task.input.get() != "OK") {
        "Fail: ${task.input.get()}"
    } else {
        "OK"
    }
}

fun `should work with assignment with apply for wrapped type`(): String {
    val task = Task(StringProperty("Fail"))
    task.apply {
        input = StringProperty("OK")
    }

    return if (task.input.get() != "OK") {
        "Fail: ${task.input.get()}"
    } else {
        "OK"
    }
}

fun `should work with extension function`(): String {
    fun StringProperty.assign(v: Int) = this.assign("OK")
    val task = Task(StringProperty("Fail"))
    task.input = 42

    return if (task.input.get() != "OK") {
        "Fail: ${task.input.get()}"
    } else {
        "OK"
    }
}

fun `should work with extension function for interface type`(): String {
    fun Property<String>.assign(v: Int) = this.set("OK")
    val task = Task(StringProperty("Fail"))
    task.input = 42

    return if (task.input.get() != "OK") {
        "Fail: ${task.input.get()}"
    } else {
        "OK"
    }
}

fun `should work with generic extension function`(): String {
    fun <T> Property<T>.assign(v: T) = this.set(v)
    data class IntProperty(var v: Int): Property<Int> {
        override fun set(v: Int) {
            this.v = v
        }
        override fun get() = this.v
    }
    data class IntTask(val input: IntProperty)
    val task = IntTask(IntProperty(0))
    task.input = 42

    return if (task.input.get() != 42) {
        "Fail: ${task.input.get()}"
    } else {
        "OK"
    }
}

fun `should work with callable receiver`(): String {
    fun StringProperty.assign(r: StringProperty.() -> Unit) = r.invoke(this)
    val task = Task(StringProperty("Fail"))
    task.input = { this.set("OK") }

    return if (task.input.get() != "OK") {
        "Fail: ${task.input.get()}"
    } else {
        "OK"
    }
}

fun `should work with Java classes`(): String {
    var taskWithField = JavaTaskWithField(StringProperty("Fail"))
    taskWithField.input = "OK"
    if (taskWithField.input.get() != "OK") return "Fail for Java: ${taskWithField.input.get()}"
    taskWithField = JavaTaskWithField(StringProperty("Fail"))
    taskWithField.input = StringProperty("OK")
    if (taskWithField.input.get() != "OK") return "Fail for Java: ${taskWithField.input.get()}"

    var taskWithProperty = JavaTaskWithProperty(StringProperty("Fail"))
    taskWithProperty.input = "OK"
    if (taskWithProperty.input.get() != "OK") return "Fail for Java: ${taskWithProperty.input.get()}"
    taskWithProperty = JavaTaskWithProperty(StringProperty("Fail"))
    taskWithProperty.input = StringProperty("OK")
    if (taskWithProperty.input.get() != "OK") return "Fail for Java: ${taskWithProperty.input.get()}"

    return "OK"
}

fun box(): String {
    var result = `should work with assignment for raw type`()
    if (result != "OK") return result

    result = `should work with assignment for wrapped type`()
    if (result != "OK") return result

    result = `should work with assignment with apply for raw type`()
    if (result != "OK") return result

    result = `should work with assignment with apply for wrapped type`()
    if (result != "OK") return result

    result = `should work with extension function`()
    if (result != "OK") return result

    result = `should work with extension function for interface type`()
    if (result != "OK") return result

    result = `should work with generic extension function`()
    if (result != "OK") return result

    result = `should work with callable receiver`()
    if (result != "OK") return result

    result = `should work with Java classes`()
    if (result != "OK") return result

    return "OK"
}
