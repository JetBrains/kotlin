// "class com.intellij.codeInspection.SuppressIntentionAction" "false"
// ACTION: Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for class Child
// ACTION: Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for file inClassHeader.kt

open class Base(s: String)
class Child: Base(""<caret>!!)