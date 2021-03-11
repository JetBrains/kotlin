// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: TYPE_MISMATCH
// !MESSAGE_TYPE: TEXT

package myPackage.a.b

class A<T> {
    fun g(t: T) {

    }

    fun <T> f(t: T) {
        g(t)
    }
}