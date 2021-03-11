// "class com.intellij.codeInspection.SuppressIntentionAction" "false"
// ACTION: Suppress 'REDUNDANT_NULLABLE' for fun foo
// ACTION: Suppress 'REDUNDANT_NULLABLE' for parameter s
// ACTION: Suppress 'REDUNDANT_NULLABLE' for file inParameterType.kt

fun foo(s: String?<caret>?) {}