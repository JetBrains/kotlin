package com.github.frankiesardo.icepick

import android.os.Bundle
import android.os.Parcelable

import org.parceler.Parcels

import icepick.Bundler

class ExampleBundler : Bundler<Any> {
    override fun put(s: String, example: Any, bundle: Bundle) {
        bundle.putParcelable(s, Parcels.wrap(example))
    }

    override fun get(s: String, bundle: Bundle): Any {
        return Parcels.unwrap<Any>(bundle.getParcelable<Parcelable>(s))
    }
}
