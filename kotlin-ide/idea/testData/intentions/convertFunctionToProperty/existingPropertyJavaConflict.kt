// SHOULD_FAIL_WITH: Method J.getFoo() already exists
package test

open class A {
    open fun <caret>foo(): Int = 1
}