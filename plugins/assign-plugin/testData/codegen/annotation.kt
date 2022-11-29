// FILE: JavaProperty.java
@ValueContainer
public interface JavaProperty<T> {
    void assign(T argument);
    T get();
}

// FILE: JavaClassStringProperty.java
@ValueContainer
public class JavaClassStringProperty {
    private String v;

    public JavaClassStringProperty(String v) {
        this.v = v;
    }

    public void assign(String v) {
        this.v = v;
    }
    public String get() {
        return v;
    }
}

// FILE: test.kt
annotation class ValueContainer

data class JavaStringProperty(private var v: String): JavaProperty<String> {
    override fun assign(v: String) {
        this.v = v
    }
    override fun get() = this.v
}

@ValueContainer
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

@ValueContainer
data class KotlinClassStringProperty(private var v: String) {
    fun assign(v: String) {
        this.v = v
    }
    fun get(): String {
        return v
    }
}

fun `should work with annotation on Java interface`(): String {
    data class Task(val input: JavaStringProperty)
    val task = Task(JavaStringProperty("Fail"))
    task.input = "OK"

    return if (task.input.get() != "OK") {
        "Fail: ${task.input.get()}"
    } else {
        "OK"
    }
}

fun `should work with annotation on Java class`(): String {
    data class Task(val input: JavaClassStringProperty)
    val task = Task(JavaClassStringProperty("Fail"))
    task.input = "OK"

    return if (task.input.get() != "OK") {
        "Fail: ${task.input.get()}"
    } else {
        "OK"
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
    var result = `should work with annotation on Java interface`()
    if (result != "OK") return result

    result = `should work with annotation on Java class`()
    if (result != "OK") return result

    result = `should work with annotation on Kotlin interface`()
    if (result != "OK") return result

    result = `should work with annotation on Kotlin class`()
    if (result != "OK") return result

    return "OK"
}
