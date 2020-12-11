// !DIAGNOSTICS: -UNUSED_VARIABLE

package mpp

import kotlin.reflect.KCallable

fun some() {
    val string: String = ""
    val any: Any = ""
    val callableRef: KCallable<*> = ::commonFun
    callableRef.name
    // should be unresolved
    callableRef.<!UNRESOLVED_REFERENCE!>call<!>()
}

fun commonFun() {
}
