// IS_APPLICABLE: false
fun foo(p: Int){}

fun bar(list: List<Int>) {
    foo(list[<caret>1])
}