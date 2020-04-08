// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(list: List<Any>, out: MutableList<String>){
    <caret>for ((i, any) in list.withIndex()) {
        if (any is String && i % 2 == 0)
            out.add(any)
    }
}