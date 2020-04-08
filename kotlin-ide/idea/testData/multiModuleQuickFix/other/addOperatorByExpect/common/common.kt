// "Add 'operator' modifier" "true"
// TOOL: org.jetbrains.kotlin.idea.inspections.AddOperatorModifierInspection

expect class Foo {
    fun <caret>unaryMinus()
}