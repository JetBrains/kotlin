// CURIOUS_ABOUT writeToParcel, createFromParcel, <clinit>
// WITH_RUNTIME

package test

import kotlinx.android.parcel.*
import android.os.*
import android.accounts.Account

@Parcelize
class Foo(val kp: Account): Parcelable