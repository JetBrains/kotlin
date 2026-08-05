// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: interface_default_properties.kt

// Defaulted interface property: a Swift conformer inherits the Kotlin default getter (`display`)
// without reimplementing it — dispatched non-virtually so it never recurses through the patched
// itable. The default's open self-call to the abstract `base` must reach the Swift override.

open class Base {
    open fun greet(): String = "Hello from Kotlin"
}
interface Labeled {
    val base: String
    val display: String get() = "display(" + base + ")"
}

fun readDisplay(l: Labeled): String = l.display
fun readBase(l: Labeled): String = l.base
