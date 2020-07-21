package foo

class A

class C<T>
class C2<T1, T2>

val v1 = C<C2<<caret>A>>()

// REF: (foo).A
