package test

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
class B(val a: A) : Parcelable {
    fun f() = a.x
}
