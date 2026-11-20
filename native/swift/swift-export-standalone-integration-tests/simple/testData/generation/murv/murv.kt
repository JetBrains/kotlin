// KIND: STANDALONE
// MODULE: main
// FILE: unmarked.kt
// Without `@MustUseReturnValues` among the containers, results are ignorable in the `check` mode.

fun unmarked(): Int = 42

fun unmarkedUnit() {}

fun unmarkedNothing(): Nothing = TODO()

fun unmarkedNullableNothing(): Nothing? = null

@MustUseReturnValues
class Greeter {
    fun greet(name: String): String = "Hello, $name"

    fun greetUnit(name: String) {}

    @IgnorableReturnValue
    fun greetIgnorable(name: String): String = "Hello, $name"

    val property: String = "property"

    class Nested {
        fun nested(): Int = 42
    }

    companion object {
        fun companionMember(): Int = 42
    }
}

@MustUseReturnValues
object MarkedObject {
    fun member(): Int = 42
}

class UnmarkedClass {
    fun member(): Int = 42

    // Overrides of `Any` members are not exported to Swift
    override fun hashCode(): Int = 42
}

// FILE: marked.kt
@file:MustUseReturnValues

fun markedByFile(): Int = 42

fun markedUnit() {}

fun markedNullableUnit(): Unit? = null

fun markedNothing(): Nothing = TODO()

fun markedNullableNothing(): Nothing? = null

fun Int.markedExtension(): Int = this

@IgnorableReturnValue
fun ignorableInMarkedFile(): Int = 42

@IgnorableReturnValue
fun <T> MutableList<T>.addAndIgnoreResult(element: T): Boolean = add(element)

class ClassInMarkedFile {
    fun member(): Int = 42
}

// FILE: overrides.kt
@MustUseReturnValues
interface MarkedGreeter {
    fun greet(name: String): String

    @IgnorableReturnValue
    fun greetIgnorable(name: String): String
}

// Overrides inherit the status of the overridden declaration
open class LoudGreeter : MarkedGreeter {
    override fun greet(name: String): String = "HELLO, $name"

    override fun greetIgnorable(name: String): String = "HELLO, $name"
}

// An override can be made ignorable explicitly
object SilentGreeter : MarkedGreeter {
    @IgnorableReturnValue
    override fun greet(name: String): String = ""

    @IgnorableReturnValue
    override fun greetIgnorable(name: String): String = ""
}

class LouderGreeter : LoudGreeter() {
    override fun greet(name: String): String = "HELLO!!!, $name"
}

interface UnmarkedBase {
    fun unmarked(): Int
}

@MustUseReturnValues
class MarkedOverridesUnmarked : UnmarkedBase {
    override fun unmarked(): Int = 42
}

// FILE: operators.kt
@MustUseReturnValues
class MarkedOperators(val value: Int) {
    operator fun plus(other: MarkedOperators): MarkedOperators = MarkedOperators(value + other.value)

    operator fun compareTo(other: MarkedOperators): Int = value.compareTo(other.value)

    operator fun get(index: Int): Int = value + index

    operator fun invoke(): Int = value
}

class UnmarkedOperators(val value: Int) {
    operator fun plus(other: UnmarkedOperators): UnmarkedOperators = UnmarkedOperators(value + other.value)

    operator fun compareTo(other: UnmarkedOperators): Int = value.compareTo(other.value)

    operator fun get(index: Int): Int = value + index

    operator fun invoke(): Int = value
}
