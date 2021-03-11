// WITH_RUNTIME
// INTENTION_TEXT: "Remove return@forEach"

fun foo() {
    listOf(1,2,3).forEach {
        <caret>return@forEach
    }
}