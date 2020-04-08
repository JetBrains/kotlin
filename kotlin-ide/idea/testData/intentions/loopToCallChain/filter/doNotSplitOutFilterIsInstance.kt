// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterIndexedTo(){}'"
// IS_APPLICABLE_2: false
fun foo(list: List<Any>, out: MutableList<Any>){
    <caret>for ((i, any) in list.withIndex()) {
        if (any is String && i % 2 == 0)
            out.add(any)
    }
}