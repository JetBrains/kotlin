interface Foo

fun foo(f: Foo, i: Int){}

fun bar() {
    foo(<caret>)
}

//ELEMENT: object
