// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: OVERLOAD_RESOLUTION_AMBIGUITY
// !MESSAGE_TYPE: HTML

package a.b

val ref1 =  ::foo

fun <T> foo(a: kotlin.String, t: T) {

}

fun <T> foo(b: String, t : T) {

}

fun foo(i: Int) {

}

class String