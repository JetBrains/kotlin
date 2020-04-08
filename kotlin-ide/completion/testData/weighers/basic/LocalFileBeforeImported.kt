package test

import some.foo2Imported

val foo5Var = 12
fun foo4CurentFile() = 12

val some = foo<caret>

// "foo" is before other elements because of exact prefix match

// ORDER: foo
// ORDER: foo5Var
// ORDER: foo4CurentFile
// ORDER: foo3FromSamePackage
// ORDER: foo2Imported
// ORDER: foo1NotImported
// INVOCATION_COUNT: 2
// SELECTED: 0