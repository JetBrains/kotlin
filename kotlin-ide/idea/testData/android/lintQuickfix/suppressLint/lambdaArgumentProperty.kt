// INTENTION_TEXT: Suppress: Add @SuppressLint("SdCardPath") annotation
// INSPECTION_CLASS: com.android.tools.idea.lint.AndroidLintSdCardPathInspection

fun foo(l: Any) = l

val bar = foo() { "<caret>/sdcard" }