import org.jetbrains.kotlin.fir.plugin.WithNestedFoo

fun <T> T.also(block: (T) -> Unit): T = this

@WithNestedFoo
class A {
    private fun test(): Foo {
        return Foo().also {
            it.hello() // should be OK
        }
    }
}

class B {
    private fun test(): Foo {
        return <!UNRESOLVED_REFERENCE!>Foo<!>().<!INAPPLICABLE_CANDIDATE!>also<!> {
            <!UNRESOLVED_REFERENCE!>it<!>.<!UNRESOLVED_REFERENCE!>hello<!>() // should be an error
        }
    }
}
