// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: inner_class_dispatch.kt

// A Kotlin `inner` class captures the outer instance, which for a Swift subclass is the dynamically created
// wrapper. Both qualified forms have to behave correctly from inside it:
//
//   this@InnerOuter.value()   virtual  -> must reach the Swift override through the patched slot
//   super@InnerOuter.value()  non-virtual -> must reach InnerOuterBase and never enter Swift
open class InnerOuterBase {
    open fun value(): String = "kotlin-base"
}

open class InnerOuter : InnerOuterBase() {
    override fun value(): String = "kotlin-outer"

    inner class Probe {
        fun viaThis(): String = this@InnerOuter.value()
        fun viaSuperOuter(): String = super@InnerOuter.value()
    }
}

fun callInnerViaThis(o: InnerOuter): String = o.Probe().viaThis()
fun callInnerViaSuperOuter(o: InnerOuter): String = o.Probe().viaSuperOuter()
