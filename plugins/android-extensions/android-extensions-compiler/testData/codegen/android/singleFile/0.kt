package test

import android.app.Activity
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.layout.*
import kotlinx.android.synthetic.clearFindViewByIdCache

class R {
    class id {
        companion object {
            const val item_detail_container = 0
            const val textView1 = 1
            const val password = 2
            const val textView2 = 3
            const val passwordConfirmation = 4
            const val login = 5
        }
    }
}

open class MyActivity(): Activity() {
    val textViewWidget = TextView(this)
    val editTextWidget = EditText(this)
    val buttonWidget = Button(this)

    override fun <T : View> findViewById(id: Int): T? {
        return when (id) {
            R.id.textView1 -> textViewWidget
            R.id.password -> editTextWidget
            R.id.login -> buttonWidget
            else -> null
        } as T?
    }

    open fun findPasswordWidget(): View = null!!

    private val textViewReadInInit = textView1
    private val passwordReadThroughOverride = findPasswordWidget()

    private fun check(expect: String, actual: String) =
        if (expect != actual) "'$actual' != '$expect'" else null

    public fun box(): String =
        check("Button", login.toString())
            ?: check("TextView", textViewReadInInit.toString())
            ?: check("EditText", passwordReadThroughOverride.toString())
            ?: "OK".also { clearFindViewByIdCache() }
}

class MyActivity2 : MyActivity() {
    override fun findPasswordWidget() = password
}

fun box(): String = MyActivity2().box()
