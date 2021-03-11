// SHOULD_FAIL_WITH: Property <b><code>foo</code></b> is overridden by declaration(s) in a subclass
open class A {
    open val <caret>foo: Int = 1
}

class B: A() {
    override val foo: Int = 2
}