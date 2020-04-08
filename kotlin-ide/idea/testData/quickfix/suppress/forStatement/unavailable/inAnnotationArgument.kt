// "class com.intellij.codeInspection.SuppressIntentionAction" "false"
// ACTION: Suppress 'INTEGER_OVERFLOW' for fun foo
// ACTION: Suppress 'INTEGER_OVERFLOW' for file inAnnotationArgument.kt

@Ann(Integer.MAX_VALUE<caret> + 1)
fun foo() {}

annotation class Ann(val b: Int)