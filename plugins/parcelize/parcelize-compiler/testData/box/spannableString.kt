// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.graphics.Color
import android.graphics.Typeface
import android.os.Parcel
import android.os.Parcelable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan

@Parcelize
data class Test(val spanned: SpannableString) : Parcelable

fun box() = parcelTest { parcel ->
    val originalSpannable = SpannableString("Hello World").apply {
        setSpan(ForegroundColorSpan(Color.RED), 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(StyleSpan(Typeface.BOLD), 6, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    val test = Test(originalSpannable)
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val test2 = parcelableCreator<Test>().createFromParcel(parcel)

    assert(test.spanned.toString() == test2.spanned.toString())

    val colorSpans = test2.spanned.getSpans(0, 5, ForegroundColorSpan::class.java)
    assert(colorSpans.size == 1)
    assert(colorSpans[0].foregroundColor == Color.RED)
    assert(test2.spanned.getSpanStart(colorSpans[0]) == 0)
    assert(test2.spanned.getSpanEnd(colorSpans[0]) == 5)

    val styleSpans = test2.spanned.getSpans(6, 11, StyleSpan::class.java)
    assert(styleSpans.size == 1)
    assert(styleSpans[0].style == Typeface.BOLD)
    assert(test2.spanned.getSpanStart(styleSpans[0]) == 6)
    assert(test2.spanned.getSpanEnd(styleSpans[0]) == 11)
}
