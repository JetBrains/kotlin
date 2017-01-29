package com.sample.icepick.lib

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import icepick.Icepick
import icepick.State
import android.util.Log

open class BaseActivity : Activity() {
    @JvmField @State
    var baseMessage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Icepick.restoreInstanceState(this, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Icepick.saveInstanceState(this, outState)
    }
}

