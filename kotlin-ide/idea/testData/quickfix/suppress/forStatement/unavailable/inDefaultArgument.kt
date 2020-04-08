// "class com.intellij.codeInspection.SuppressIntentionAction" "false"
// ACTION: Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for fun foo
// ACTION: Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for parameter s
// ACTION: Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for file inDefaultArgument.kt

fun foo(s: String = ""<caret>!!) {}