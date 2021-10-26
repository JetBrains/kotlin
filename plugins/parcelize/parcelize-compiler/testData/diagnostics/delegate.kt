package test

import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import android.os.Parcel

open class Delegate : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {}
    override fun describeContents() = 0
}

@Parcelize
class Test : Parcelable <!PARCELABLE_DELEGATE_IS_NOT_ALLOWED!>by<!> Delegate()
