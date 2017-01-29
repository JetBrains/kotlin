package com.github.frankiesardo.icepick

import android.os.Bundle

import icepick.Bundler

class MyBundler : Bundler<String> {
    override fun put(key: String, value: String?, bundle: Bundle) {
        if (value != null) {
            bundle.putString(key, value + "*")
        }
    }

    override fun get(key: String, bundle: Bundle): String? {
        return bundle.getString(key)
    }
}
