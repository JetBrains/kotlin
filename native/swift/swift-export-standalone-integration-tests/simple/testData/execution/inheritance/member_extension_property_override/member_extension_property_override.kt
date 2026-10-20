// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: member_extension_property_override.kt

// Member extension properties and properties with context parameters are exported as `getX(...)`/`setX(...)` functions.
// Kotlin has to dispatch their accessors to Swift overrides, too.

open class PropertyAnchor

class PropCtx(val tag: String)

interface LengthView {
    val String.len: Int
    var String.slot: String
    val String.doubled: String get() = this + this
    context(ctx: PropCtx) val String.scoped: String
}

fun readLen(value: LengthView, s: String): Int = with(value) { s.len }
fun readSlot(value: LengthView, s: String): String = with(value) { s.slot }
fun writeSlot(value: LengthView, s: String, newValue: String) { with(value) { s.slot = newValue } }
fun readDoubled(value: LengthView, s: String): String = with(value) { s.doubled }
fun readScoped(value: LengthView, s: String): String = with(value) { context(PropCtx("ctx")) { s.scoped } }

// Same name, different extension receivers.
interface OverloadedView {
    val Int.describe: String
    val String.describe: String
}

fun describeInt(value: OverloadedView, x: Int): String = with(value) { x.describe }
fun describeString(value: OverloadedView, s: String): String = with(value) { s.describe }

// A context parameter without an extension receiver.
interface ContextualLabel {
    context(ctx: PropCtx) val label: String
}

fun readLabel(value: ContextualLabel): String = context(PropCtx("ctx")) { value.label }

open class OpenLengthView {
    private val storage = mutableMapOf<String, String>()

    open val String.len: Int get() = length
    open var String.slot: String
        get() = storage[this] ?: "kotlin-none"
        set(newValue) { storage[this] = newValue }
}

fun readOpenLen(value: OpenLengthView, s: String): Int = with(value) { s.len }
fun readOpenSlot(value: OpenLengthView, s: String): String = with(value) { s.slot }
fun writeOpenSlot(value: OpenLengthView, s: String, newValue: String) { with(value) { s.slot = newValue } }
