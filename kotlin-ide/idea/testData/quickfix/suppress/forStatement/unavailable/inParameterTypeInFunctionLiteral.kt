// "class com.intellij.codeInspection.SuppressIntentionAction" "false"
// ACTION: Suppress 'REDUNDANT_NULLABLE' for fun foo
// ACTION: Suppress 'REDUNDANT_NULLABLE' for parameter x
// ACTION: Suppress 'REDUNDANT_NULLABLE' for file inParameterTypeInFunctionLiteral.kt

fun foo() {
    any {
        x: String?<caret>? ->
    }
}

fun any(a: Any?) {}