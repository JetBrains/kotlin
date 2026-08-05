// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: class_and_interface_properties.kt

// Property accessors are their own slots: Swift Export emits a separate reverse bridge per getter and setter.

interface Counter { var count: Int }

open class CounterBase : Counter { override var count: Int = 0 }

fun setCount(c: Counter, n: Int) { c.count = n }
fun getCount(c: Counter): Int = c.count

// Open class with a get-only (`val`) and a settable (`var`) property, both overridable from Swift.
// Kotlin-side access (readLabel/readNick/writeNick) must reach the Swift accessors via the patched vtable.
open class Named {
    open val label: String = "kotlin-label"
    open var nick: String = "kotlin-nick"
}

fun readLabel(n: Named): String = n.label
fun readNick(n: Named): String = n.nick
fun writeNick(n: Named, v: String) { n.nick = v }

// Property analog of `Vehicle`: a Swift override reading `super.title` must reach the inherited Kotlin
// getter via the non-virtual `_direct` bridge (no recursion); a non-overridden `rank` reached from
// Kotlin must also route through the direct bridge instead of looping through its patched slot.
open class Book {
    open val title: String = "kotlin-title"
    open val rank: Int = 1
}

fun readTitle(b: Book): String = b.title
fun readRank(b: Book): Int = b.rank

