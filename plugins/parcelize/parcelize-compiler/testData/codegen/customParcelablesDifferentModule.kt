// CURIOUS_ABOUT writeToParcel, createFromParcel, <clinit>
// WITH_RUNTIME

package test

import kotlinx.parcelize.*
import android.os.*
import android.accounts.Account

@Parcelize
class Foo(val kp: Account): Parcelable