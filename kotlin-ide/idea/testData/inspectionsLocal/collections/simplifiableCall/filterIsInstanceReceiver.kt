// WITH_RUNTIME
fun List<Any>.test() {
    <caret>filter { it is String }
}