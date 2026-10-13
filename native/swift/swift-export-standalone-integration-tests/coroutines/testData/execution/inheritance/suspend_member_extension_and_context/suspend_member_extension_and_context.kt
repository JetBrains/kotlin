// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Main
// FILE: suspend_member_extension_and_context.kt

// Reverse bridges take the parameters of the overridden method in its order (context parameters before the extension receiver),
// with the continuation of the lowered suspend function last.

class CtxA(val tag: String)
class CtxB(val tag: String)

open class AsyncMemberExtensions {
    open suspend fun Int.measure(): String = "kotlin-int($this)"
    open suspend fun String.measure(): String = "kotlin-string($this)"

    context(a: CtxA, b: CtxB)
    open suspend fun String.combine(count: Int): String = "kotlin-combine(${a.tag}, ${b.tag}, $this, $count)"

    context(a: CtxA)
    open suspend fun tag(): String = "kotlin-tag-a(${a.tag})"

    context(b: CtxB)
    open suspend fun tag(): String = "kotlin-tag-b(${b.tag})"
}

suspend fun measureInt(o: AsyncMemberExtensions, x: Int): String = with(o) { x.measure() }
suspend fun measureString(o: AsyncMemberExtensions, s: String): String = with(o) { s.measure() }
suspend fun combine(o: AsyncMemberExtensions, s: String, count: Int): String =
    with(o) { context(CtxA("a"), CtxB("b")) { s.combine(count) } }
suspend fun tagA(o: AsyncMemberExtensions): String = with(o) { context(CtxA("a")) { tag() } }
suspend fun tagB(o: AsyncMemberExtensions): String = with(o) { context(CtxB("b")) { tag() } }
