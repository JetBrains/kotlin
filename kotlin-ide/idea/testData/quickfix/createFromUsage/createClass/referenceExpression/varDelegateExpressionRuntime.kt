// "Create object 'Foo'" "true"
// DISABLE-ERRORS

open class B

class A {
    var x: B by <caret>Foo
}
