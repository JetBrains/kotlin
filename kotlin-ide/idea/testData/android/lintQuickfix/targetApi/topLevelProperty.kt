// INTENTION_TEXT: Add @TargetApi(M) Annotation
// INSPECTION_CLASS: com.android.tools.idea.lint.AndroidLintNewApiInspection
import android.app.Activity

val top: Int
    get() = Activity().<caret>checkSelfPermission(READ_CONTACTS)