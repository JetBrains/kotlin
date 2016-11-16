// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintLocalSuppressInspection

import android.annotation.SuppressLint
import android.view.View

@Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
class WrongAnnotation2 {
    @SuppressLint("NewApi")
    private val field1: Int = 0

    @SuppressLint("NewApi")
    private val field2 = 5

    companion object {
        @SuppressLint("NewApi") // Valid: class-file check on method
        fun foobar(view: View, @SuppressLint("NewApi") foo: Int) {
            // Invalid: class-file check
            @SuppressLint("NewApi") // Invalid
            val a: Boolean
            @SuppressLint("SdCardPath", "NewApi") // Invalid: class-file based check on local variable
            val b: Boolean
            @android.annotation.SuppressLint("SdCardPath", "NewApi") // Invalid (FQN)
            val c: Boolean
            @SuppressLint("SdCardPath") // Valid: AST-based check
            val d: Boolean
        }

        init {
            // Local variable outside method: invalid
            @SuppressLint("NewApi")
            val localvar = 5
        }

        private fun test() {
            @SuppressLint("NewApi") // Invalid
            val a = View.MEASURED_STATE_MASK
        }
    }
}
