package com.github.frankiesardo.icepick

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import com.sample.icepick.lib.BaseCustomView
import icepick.State

class CustomView : BaseCustomView {
    @JvmField @State
    var textColor: Int? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun setTextColorWithAnotherMethod(color: Int) {
        this.textColor = color
        setTextColor(textColor!!)
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        super.onRestoreInstanceState(state)
        if (textColor != null) {
            setTextColorWithAnotherMethod(textColor!!)
        }
    }
}
