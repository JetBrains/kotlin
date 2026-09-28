// WITH_STDLIB
// ISSUE: KT-89040

import lombok.Builder

class Result(val value: String)
class Ctx(val tag: String)

// A `@Builder` function's builder class is nested in the class that declares the function, and
// `AbstractBuilderGenerator` only ever walks a class's own declarations and its companion object's. A top-level
// function has no such class, so nothing is generated for one and the annotation is reported the way every
// other shape the plugin cannot act upon is. Java's closest analogue, a `static` method, always has a class to
// hold the builder.
<!ANNOTATION_HAS_NO_EFFECT!>@Builder<!>
fun topLevelFunction(): Int = 42

context(ctx: Ctx)
<!ANNOTATION_HAS_NO_EFFECT!>@Builder<!>
fun String.bothReceiverAndContext(n: Int): Result = Result("${ctx.tag}/$this/$n")

<!ANNOTATION_HAS_NO_EFFECT!>@Builder<!>
fun topLevelInferred(value: Int) = Result(value.toString())

// A local function is in the same position: the class its builder would be nested in doesn't exist either.
// This is the counterpart of the local *class* a few cases up in builderChecks_kotlin.kt.
fun withLocalFunction() {
    <!ANNOTATION_HAS_NO_EFFECT!>@Builder<!>
    fun local(value: Int): Result = Result(value.toString())

    local(1)
}

fun useTopLevel() {
    <!UNRESOLVED_REFERENCE!>builder<!>() // Nothing is generated
}
