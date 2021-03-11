class Foo() {
    val s = th<caret>is@Foo.toString()
}

// 'this' should not be listed, only 'this@Foo'
/*
this@Foo
this@Foo.toString()
*/
