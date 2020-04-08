package test

import test.foo

val String.<caret>foo = "foo"

class B {
    val ref = "".foo
}