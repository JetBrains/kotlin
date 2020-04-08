// "Suppress 'ConstantConditionIf' for fun foo" "true"

fun foo() {
    if (<caret>true) {
    }
}

// TOOL: org.jetbrains.kotlin.idea.inspections.ConstantConditionIfInspection