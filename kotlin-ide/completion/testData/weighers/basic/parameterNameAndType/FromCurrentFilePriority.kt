class MyFileA
class MyFileB
class MyFileC

fun f(myFileB: MyFileB, myFileX: Int, myFileY: Int)
fun g(myFileY: Int)
fun h(myFileX: String)

fun foo(myFi<caret>)

// ORDER: myFileY : Int
// ORDER: myFileB : MyFileB
// ORDER: myFileX : Int
// ORDER: myFileX : String
// ORDER: myFileA : MyFileA
// ORDER: myFileC : MyFileC
