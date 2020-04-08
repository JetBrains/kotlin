// "Create class 'A'" "true"
// ERROR: Unresolved reference: A
class B {

}

class Foo: J.<caret>A(1, "2", B()) {

}