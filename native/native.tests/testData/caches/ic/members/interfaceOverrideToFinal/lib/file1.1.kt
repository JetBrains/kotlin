package test

interface I { fun f(): Int }
open class C : I {
    final override fun f(): Int = 2
}
