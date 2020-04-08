// "Change type to mutable" "false"
// TOOL: org.jetbrains.kotlin.idea.inspections.SuspiciousCollectionReassignmentInspection
// ACTION: Replace overloaded operator with function call
// ACTION: Replace with ordinary assignment
// WITH_RUNTIME
fun test() {
    var list: List<Int>
    list = listOf(1)
    list +=<caret> 2
}