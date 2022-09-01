// FILE: Property.java
@ValueContainer
public interface Property<T> {
    void set(T v);
    T get();
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

fun box(): String {
    run {
        // Should work with assignment for raw type
        val task = Task(StringProperty("Fail"))
        task.input = "OK"
        if (task.input.get() != "OK") return "Fail: ${task.input.get()}"
    }

    run {
        // Should work with assignment for wrapped type
        val task = Task(StringProperty("Fail"))
        task.input = StringProperty("OK")
        if (task.input.get() != "OK") return "Fail: ${task.input.get()}"
    }

    run {
        // Should work with assignment with apply for raw type
        val task = Task(StringProperty("Fail"))
        task.apply {
            input = "OK"
        }
        if (task.input.get() != "OK") return "Fail: ${task.input.get()}"
    }

    run {
        // Should work with assignment with apply for wrapped type
        val task = Task(StringProperty("Fail"))
        task.apply {
            input = StringProperty("OK")
        }
        if (task.input.get() != "OK") return "Fail: ${task.input.get()}"
    }

    run {
        // Should work with extension function
        fun StringProperty.assign(v: Int) = this.assign("OK")
        val task = Task(StringProperty("Fail"))
        task.input = 42
        if (task.input.get() != "OK") return "Fail: ${task.input.get()}"
    }

    run {
        // Should work with extension function for interface type
        fun Property<String>.assign(v: Int) = this.set("OK")
        val task = Task(StringProperty("Fail"))
        task.input = 42
        if (task.input.get() != "OK") return "Fail: ${task.input.get()}"
    }

    run {
        // Should work with generic extension function
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
        if (task.input.get() != 42) return "Fail: ${task.input.get()}"
    }

    run {
        // Should work with callable receiver
        fun StringProperty.assign(r: StringProperty.() -> Unit) = r.invoke(this)
        val task = Task(StringProperty("Fail"))
        task.input = { this.set("OK") }
        if (task.input.get() != "OK") return "Fail: ${task.input.get()}"
    }
    return "OK"
}
