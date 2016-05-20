// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintShowToastInspection

import android.app.Activity
import android.content.Context
import android.widget.Toast

@Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
class ToastTest(context: Context) : Activity() {
    private fun createToast(context: Context): Toast {
        // Don't warn here
        return Toast.makeText(context, "foo", Toast.LENGTH_LONG)
    }

    private fun showToast(context: Context) {
        // Don't warn here
        val toast = Toast.makeText(context, "foo", Toast.LENGTH_LONG)
        System.out.println("Other intermediate code here")
        val temp = 5 + 2
        toast.show()
    }

    private fun showToast2(context: Context) {
        // Don't warn here
        val duration = Toast.LENGTH_LONG
        Toast.makeText(context, "foo", Toast.LENGTH_LONG).show()
        Toast.makeText(context, R.string.app_name, duration).show()
    }

    private fun broken(context: Context) {
        // Errors
        Toast.<warning descr="Toast created but not shown: did you forget to call `show()` ?">makeText(context, "foo", Toast.LENGTH_LONG)</warning>
        val toast = Toast.<warning descr="Toast created but not shown: did you forget to call `show()` ?">makeText(context, R.string.app_name, <warning descr="Expected duration `Toast.LENGTH_SHORT` or `Toast.LENGTH_LONG`, a custom duration value is not supported">5000</warning>)</warning>
        toast.duration
    }

    init {
        Toast.<warning descr="Toast created but not shown: did you forget to call `show()` ?"><warning descr="Toast created but not shown: did you forget to call `show()` ?">makeText(context, "foo", Toast.LENGTH_LONG)</warning></warning>
    }

    @android.annotation.SuppressLint("ShowToast")
    private fun checkSuppress1(context: Context) {
        val toast = Toast.makeText(this, "MyToast", Toast.LENGTH_LONG)
    }

    private fun checkSuppress2(context: Context) {
        @android.annotation.SuppressLint("ShowToast")
        val toast = Toast.makeText(this, "MyToast", Toast.LENGTH_LONG)
    }

    class R {
        object string {
            val app_name = 1
        }
    }
}