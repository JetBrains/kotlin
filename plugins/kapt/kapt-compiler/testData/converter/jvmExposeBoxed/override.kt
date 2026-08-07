// WITH_STDLIB

// The boxed variant of an exposed override has no counterpart in the supertype, because interface members can
// never be exposed. It must therefore not carry '@java.lang.Override' in the stub - with the annotation
// present the generated stub does not compile ("does not override or implement a method from a supertype").
// 'ControlDerived' is the control: its override has a real counterpart, so '@Override' belongs on it.
// TODO: Remove if green after the fix

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

interface Base {
    fun transform(id: Id): Id
}

class Derived : Base {
    @JvmExposeBoxed
    override fun transform(id: Id): Id = id
}

open class ControlBase {
    open fun plain(s: String): String = s
}

class ControlDerived : ControlBase() {
    override fun plain(s: String): String = s
}
