package test

import kotlinx.android.parcel.Parcelize
import android.os.Parcelable
import android.os.Parcel

open class Delegate : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {}
    override fun describeContents() = 0
}

@Parcelize
class Test : Parcelable <error descr="[PLUGIN_ERROR] Delegating 'Parcelable' is not allowed">by</error> Delegate()
