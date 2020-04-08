// IS_APPLICABLE: false
interface I {
    public fun foo()
}

abstract class C : I {
    <caret>public override fun foo() {}
}