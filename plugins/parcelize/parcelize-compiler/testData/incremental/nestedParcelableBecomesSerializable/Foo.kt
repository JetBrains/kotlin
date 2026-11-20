package test

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
class Foo(val x: Int) : Parcelable
