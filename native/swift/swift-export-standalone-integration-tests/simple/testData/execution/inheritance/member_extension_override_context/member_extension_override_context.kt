// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: member_extension_override_context.kt

// KT-89699: reverse bridges place context parameters after the regular ones, overloads have to be bound by them too.

open class ContextAnchor

class CtxA(val tag: String)
class CtxB(val tag: String)

// Overloads by extension receiver, each with a context parameter.
interface ContextualMeasurer {
    context(ctx: CtxA) fun Int.scaled(): String
    context(ctx: CtxA) fun String.scaled(): String
}

fun scaledInt(value: ContextualMeasurer, x: Int): String = with(value) { context(CtxA("a")) { x.scaled() } }
fun scaledString(value: ContextualMeasurer, s: String): String = with(value) { context(CtxA("a")) { s.scaled() } }

// Overloads that differ only in the context parameter.
interface ContextOnlyOverloads {
    context(ctx: CtxA) fun tag(): String
    context(ctx: CtxB) fun tag(): String
}

fun tagA(value: ContextOnlyOverloads): String = with(value) { context(CtxA("a")) { tag() } }
fun tagB(value: ContextOnlyOverloads): String = with(value) { context(CtxB("b")) { tag() } }
