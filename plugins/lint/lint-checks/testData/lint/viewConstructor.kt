// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintViewConstructorInspection

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView

class View1(context: Context?) : View(context)
class View2(context: Context?, attrs: AttributeSet?) : View(context, attrs)
class View3(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : TextView(context, attrs, defStyleAttr)

// Error
class <warning descr="Custom view `View4` is missing constructor used by tools: `(Context)` or `(Context,AttributeSet)` or `(Context,AttributeSet,int)`">View4</warning>(<warning descr="[UNUSED_PARAMETER] Parameter 'int' is never used">int</warning>: Int, context: Context?) : View(context)

// Error
class <warning descr="Custom view `View5` is missing constructor used by tools: `(Context)` or `(Context,AttributeSet)` or `(Context,AttributeSet,int)`">View5</warning>(context: Context?, attrs: AttributeSet?, val name: String) : View(context, attrs)

class View6 : View {
    constructor(context: Context) : super(context) {

    }
}

class View7 : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
}

// Error
class <warning descr="Custom view `View8` is missing constructor used by tools: `(Context)` or `(Context,AttributeSet)` or `(Context,AttributeSet,int)`">View8</warning> : View {
    constructor(context: Context, <warning descr="[UNUSED_PARAMETER] Parameter 'a' is never used">a</warning>: Int) : super(context)
}