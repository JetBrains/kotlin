// "Remove redundant ''@TypeParceler'' annotation" "true"
// WARNING: This annotation duplicates the one for Class 'Foo'
// WITH_RUNTIME

package com.myapp.activity

import android.os.*
import kotlinx.android.parcel.*

object StringParceler : Parceler<String> {
    override fun create(parcel: Parcel) = TODO()
    override fun String.write(parcel: Parcel, flags: Int) = TODO()
}

@Parcelize
@TypeParceler<String, StringParceler>
class Foo(@<caret>TypeParceler<String, StringParceler> val a: String) : Parcelable