// "Surround with null check" "true"
// WITH_RUNTIME

fun foo(list: List<String>?) {
    for (element in <caret>list) {}
}