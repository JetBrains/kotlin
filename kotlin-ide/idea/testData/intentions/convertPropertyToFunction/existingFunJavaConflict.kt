// SHOULD_FAIL_WITH: Method J.foo() already exists
package test

open class A {
    open val <caret>foo: Int = 1
}