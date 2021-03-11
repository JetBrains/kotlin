// !DIAGNOSTICS_NUMBER: 2
// !DIAGNOSTICS: RETURN_TYPE_MISMATCH_ON_OVERRIDE
// !MESSAGE_TYPE: HTML

package myPackage.a.b

open class A {
    open fun f(): kotlin.String = "asd"
    open fun g(): kotlin.String = "Asd"
}

class B : A() {
    override fun f(): String = String()
    override fun g(): kotlin.Int = 3
}

class String