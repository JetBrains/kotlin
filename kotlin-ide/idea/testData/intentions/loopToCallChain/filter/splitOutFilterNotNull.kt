// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterNotNull().filter{}.forEach{}'"
// INTENTION_TEXT_2: "Replace with 'asSequence().filterNotNull().filter{}.forEach{}'"
fun foo(list: List<String?>){
    <caret>for (l in list) {
        if (l != null && l.startsWith("IMG:"))
            println(l.hashCode())
    }
}