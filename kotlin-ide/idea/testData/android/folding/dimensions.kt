package com.myapp

import <fold text='...' expand='false'>android.app.Activity
import android.app.AlertDialog</fold>

class MyActivity : Activity() <fold text='{...}' expand='true'>{
    fun test() <fold text='{...}' expand='true'>{
        val dialog = AlertDialog.Builder(this)
                .setView(null)
                .setPositiveButton(<fold text='"Yes"' expand='false'>R.string.positive_button_text</fold>, null)
        .create()
        val dimension = <fold text='56dip' expand='false'>resources.getDimension(R.dimen.action_bar_default_height)</fold>
        val dimension2 = <fold text='4dip' expand='false'>resources.getDimensionPixelOffset(R.dimen.action_bar_icon_vertical_padding)</fold>
        val dimension3 = <fold text='3dip' expand='false'>resources.getDimensionPixelSize(R.dimen.mydimen3)</fold>
        val strings = intArrayOf(<fold text='"1111"' expand='false'>R.string.string1</fold>, <fold text='"2222"' expand='false'>R.string.string2</fold>, <fold text='"3333"' expand='false'>R.string.string3</fold>)
        val dimensions = intArrayOf(<fold text='1dip' expand='false'>R.dimen.mydimen1</fold>, <fold text='2dip' expand='false'>R.dimen.mydimen2</fold>)
        val maxButtons = <fold text='max_action_buttons: 2' expand='false'>resources.getInteger(R.integer.max_action_buttons)</fold>
        val dimensionWithReference = <fold text='1dip' expand='false'>resources.getDimension(R.dimen.dimenRef)</fold>
        val dimensionWithAndroidReference = <fold text='@android:dimen/app_icon_size' expand='false'>resources.getDimension(R.dimen.dimenAndroidRef)</fold>
        with(resources) <fold text='{...}' expand='true'>{
            val dimension = <fold text='56dip' expand='false'>resources.getDimension(R.dimen.action_bar_default_height)</fold>.toInt()
        }</fold>
    }</fold>
}</fold>
