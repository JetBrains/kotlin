// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintWrongCallInspection

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.LinearLayout

abstract class WrongViewCall(context: Context, attrs: AttributeSet, defStyle: Int) : LinearLayout(context, attrs, defStyle) {
    private val child: MyChild? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        child?.<error descr="Suspicious method call; should probably call \"`draw`\" rather than \"`onDraw`\"">onDraw</error>(canvas)
    }

    private inner class MyChild(context: Context, attrs: AttributeSet, defStyle: Int) : FrameLayout(context, attrs, defStyle) {

        public override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
        }
    }
}
