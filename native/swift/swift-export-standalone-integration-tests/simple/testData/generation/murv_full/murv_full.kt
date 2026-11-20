// KIND: STANDALONE
// FREE_COMPILER_ARGS: -Xreturn-value-checker=full
// MODULE: dependency
// EXPORT_TO_SWIFT
// FILE: dependency.kt
// In the `full` mode, every function is must-use unless it is annotated with `@IgnorableReturnValue`.
// The status is stored in the klib, so it stays the same when the module is consumed as a dependency.

package dependency

fun dependencyFunction(): Int = 42

@IgnorableReturnValue
fun ignorableDependencyFunction(): Int = 42

open class DependencyBase {
    open fun checked(): Int = 42

    @IgnorableReturnValue
    open fun ignorable(): Int = 42
}

// MODULE: main(dependency)
// FILE: main.kt
import dependency.DependencyBase

fun function(): Int = 42

fun unitFunction() {}

fun nothingFunction(): Nothing = TODO()

@IgnorableReturnValue
fun ignorable(): Int = 42

class Derived : DependencyBase() {
    override fun checked(): Int = 1

    // In the `full` mode, an override of an ignorable function stays ignorable
    override fun ignorable(): Int = 1
}

class Foo {
    val property: Int = 42

    fun member(): Int = 42

    @IgnorableReturnValue
    fun ignorableMember(): Int = 42
}
