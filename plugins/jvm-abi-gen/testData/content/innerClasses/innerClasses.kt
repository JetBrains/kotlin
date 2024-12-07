@Anno(Outer.Middle.Inner::class)
class InAnnotation {}

class InPublicMethod {
    fun foo(x: Outer.Middle.Inner): Class<*> = OuterKt.MiddleKt.InnerKt::class.java
}

class InPrivateMethod {
    private fun foo(x: Outer.Middle.Inner): Class<*> = OuterKt.MiddleKt.InnerKt::class.java
}

class InInlineMethod {
    inline fun foo(x: Outer.Middle.Inner): Class<*> = OuterKt.MiddleKt.InnerKt::class.java
}

class OuterKt {
    class MiddleKt {
        class InnerKt
    }
}
