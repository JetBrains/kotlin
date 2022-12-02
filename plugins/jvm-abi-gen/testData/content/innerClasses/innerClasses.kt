@Anno(Outer.Middle.Inner::class)
class InAnnotation {}

class InPublicMethod {
    fun foo(x: Outer.Middle.Inner) {}
}

class InPrivateMethod {
    private fun foo(x: Outer.Middle.Inner) {}
}

class InInlineMethod {
    inline fun foo(): Class<*> = Outer.Middle.Inner::class.java
}
