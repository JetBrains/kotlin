package foo

class A

class C<T>
class C1<T>

class D: C<C1<A, <caret>A>>()

// REF: (foo).A
