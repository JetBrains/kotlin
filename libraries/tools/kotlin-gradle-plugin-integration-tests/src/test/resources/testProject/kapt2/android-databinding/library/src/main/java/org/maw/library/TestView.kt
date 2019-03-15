package org.maw.library

import android.content.Context
import android.databinding.BindingAdapter
import android.databinding.DataBindingComponent
import android.databinding.DataBindingUtil
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import org.maw.library.databinding.TestViewBinding

class TestView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        DataBindingUtil.inflate<TestViewBinding>(
            LayoutInflater.from(context),
            R.layout.test_view,
            this,
            true,
            object : DataBindingComponent {
                override fun getTextLabelBindingAdapter(): TextLabelBindingAdapter {
                    return TextLabelBindingAdpaterImpl()
                }
            }
        )
    }
}

interface TextLabelBindingAdapter {
    @BindingAdapter("textLabel")
    fun setTextLabel(
        errorMessageView: TextView,
        textLabelType: Int
    )
}

class TextLabelBindingAdpaterImpl : TextLabelBindingAdapter {
    override fun setTextLabel(
        errorMessageView: TextView,
        textLabelType: Int
    ) {
        when (textLabelType) {
            0 -> errorMessageView.text = "Zero"
            1 -> errorMessageView.text = "One"
            2 -> errorMessageView.text = "Two"
        }
    }

}