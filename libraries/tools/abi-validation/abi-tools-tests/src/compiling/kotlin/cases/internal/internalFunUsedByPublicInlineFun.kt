// ISSUE: KT-81432
package cases.internal

class InternalFunUsedByPublicInlineFun {
    internal fun internalFun() = Unit

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    inline fun publicInline() {
        internalFun()
    }
}
