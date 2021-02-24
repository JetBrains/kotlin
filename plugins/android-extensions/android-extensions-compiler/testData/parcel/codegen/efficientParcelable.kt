// CURIOUS_ABOUT writeToParcel, createFromParcel, <clinit>
// WITH_RUNTIME
//FILE: test.kt
package test

import kotlinx.android.parcel.*
import android.os.Parcelable

@Parcelize
class Foo(val bar: Bar): Parcelable

@Parcelize
class Bar(val foo: Foo?) : Parcelable
