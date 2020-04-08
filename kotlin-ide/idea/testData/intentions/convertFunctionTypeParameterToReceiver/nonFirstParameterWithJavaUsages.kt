// SHOULD_FAIL_WITH: Can't replace non-Kotlin reference with call expression: super.foo
open class K {
    open fun foo(f: (Int, <caret>Boolean) -> String) {

    }
}