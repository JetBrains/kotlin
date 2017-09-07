package test

import kotlinx.android.parcel.Parcelize
import android.os.Parcelable
import android.os.Parcel

open class Delegate : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {}
    override fun describeContents() = 0
}

@Parcelize
class Test : Parcelable <error descr="[PARCELABLE_DELEGATE_IS_NOT_ALLOWED] Delegating 'Parcelable' is now allowed">by</error> Delegate()