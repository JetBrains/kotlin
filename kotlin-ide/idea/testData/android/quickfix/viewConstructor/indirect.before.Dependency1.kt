// "Add Android View constructors using '@JvmOverloads'" "true"
// ERROR: The type has a constructor, and thus must be initialized here

package android.view

import android.util.AttributeSet
import android.content.Context

public open class View {
    constructor(context: Context)
    constructor(context: Context, attrs: AttributeSet?)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
}

public open class TextView : View {
    constructor(context: Context) super(context)
    constructor(context: Context, attrs: AttributeSet?) super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) super(context, attrs, defStyleAttr)
}