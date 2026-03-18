// ISSUE: KT-81432
package cases.internal

class InternalFunUsedByPublicInlineFun {
    internal fun internalFun() = Unit

    @Suppress("NOTHING_TO_INLINE", "NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    inline fun publicInline() {
        internalFun()
    }
}
