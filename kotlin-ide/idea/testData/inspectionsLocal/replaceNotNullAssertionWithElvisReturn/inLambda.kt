// WITH_RUNTIME
fun test(list: List<String>, number: Int?) {
    list.forEach {
        number!!<caret>
    }
}