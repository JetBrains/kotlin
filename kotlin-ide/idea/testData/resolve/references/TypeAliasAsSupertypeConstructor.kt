package test

open class C

typealias CA = C

class D : <caret>CA()

// REF: (test).CA
