package test

interface I { fun f(): Int }
open class C : I {
    override fun f(): Int = 1
}
