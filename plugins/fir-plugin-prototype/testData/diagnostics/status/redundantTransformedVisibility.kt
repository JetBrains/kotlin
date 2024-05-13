// WITH_EXTENDED_CHECKERS
// ISSUE: KT-54496

import org.jetbrains.kotlin.fir.plugin.AllPublic
import org.jetbrains.kotlin.fir.plugin.Visibility

@AllPublic(Visibility.Protected)
class A {
    <!REDUNDANT_VISIBILITY_MODIFIER!>protected<!> val redundantProtectedProp: String = ""
    public val publicProp: String = ""
    private val privateProp: String = ""

    <!REDUNDANT_VISIBILITY_MODIFIER!>protected<!> fun redundantProtectedFun() {}
    public fun publicFun() {}
    private fun privateFun() {}
}

@AllPublic(Visibility.Private)
class B {
    protected val protectedProp: String = ""
    public val publicProp: String = ""
    <!REDUNDANT_VISIBILITY_MODIFIER!>private<!> val redundantPrivateProp: String = ""

    protected fun protectedFun() {}
    public fun publicFun() {}
    <!REDUNDANT_VISIBILITY_MODIFIER!>private<!> fun redundantPrivateFun() {}
}
