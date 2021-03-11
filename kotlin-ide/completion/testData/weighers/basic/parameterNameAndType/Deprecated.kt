class MyClassA
@Deprecated class MyClassB
class MyClassC

fun foo(myCla<caret>)

// ORDER: myClassA : MyClassA
// ORDER: myClassC : MyClassC
// ORDER: myClassB : MyClassB
