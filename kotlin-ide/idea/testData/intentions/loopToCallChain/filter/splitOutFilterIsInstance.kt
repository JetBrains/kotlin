// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterIsInstance<>().filterTo(){}'"
// INTENTION_TEXT_2: "Replace with 'asSequence().filterIsInstance<>().filterTo(){}'"
fun foo(list: List<Any>, out: MutableList<String>){
    <caret>for (any in list) {
        if (any is String && any.length > 0)
            out.add(any)
    }
}