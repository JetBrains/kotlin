// DISABLE_IR_TYPE_PARAMETER_SCOPE_CHECKS: ANY
// Reason: https://issuetracker.google.com/issues/524008575
// CURIOUS_ABOUT: writeToParcel, createFromParcel, <clinit>
// WITH_STDLIB
//FILE: test.kt
package test

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
class Foo(val bar: Bar): Parcelable

@Parcelize
class Bar(val foo: Foo?) : Parcelable
