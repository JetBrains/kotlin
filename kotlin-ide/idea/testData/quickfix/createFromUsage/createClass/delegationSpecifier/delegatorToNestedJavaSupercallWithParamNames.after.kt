// "Create class 'A'" "true"
// ERROR: Unresolved reference: A
class B {

}

class Foo: J.A(abc = 1, ghi = "2", def = B()) {

}