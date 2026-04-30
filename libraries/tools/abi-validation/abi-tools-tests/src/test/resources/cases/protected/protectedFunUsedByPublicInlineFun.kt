// ISSUE: KT-81432
package cases.protected

class ProtectedFunUsedByPublicInlineFun {
    protected fun protectedFun() = Unit

    @Suppress("NOTHING_TO_INLINE", "PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR")
    inline fun publicInline() {
        protectedFun()
    }
}

open class OpenProtectedFunUsedByPublicInlineFun {
    protected fun protectedFun() = Unit

    @Suppress("NOTHING_TO_INLINE", "PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR")
    inline fun publicInline() {
        protectedFun()
    }
}
