// FIR_IDENTICAL
package test

import kotlinx.parcelize.Parcelize
import android.os.Parcel
import android.os.Parcelable

@Suppress("UNUSED_PARAMETER")
class User(firstName: String, secondName: String, val age: Int) : Parcelable {
    override fun writeToParcel(p0: Parcel?, p1: Int) {}
    override fun describeContents() = 0
}