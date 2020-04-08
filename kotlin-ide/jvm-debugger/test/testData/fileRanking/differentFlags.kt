// DO_NOT_CHECK_CLASS_FQNAME

//FILE: a/a.kt
package a

class A private constructor() {
    protected fun a() {
        val a = 5
    }
}

//FILE: b/a.kt

//significant whitespace
package b

class A public constructor() {
    private fun a() {
        val a = 5
    }
}