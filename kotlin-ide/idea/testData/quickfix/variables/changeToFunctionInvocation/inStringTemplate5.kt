// "Change to function invocation" "true"
fun bar(i: Int, j: Int) {}

fun test(s: String){
    "$bar<caret>(1, 2) sometext $s"
}