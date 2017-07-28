package test

import kotlinx.android.parcel.Parcelize
import android.os.Parcelable
import android.os.Parcel

@Parcelize
class A(val a: String) : Parcelable {
    companion object {
        @JvmField
        val <error descr="[PLUGIN_ERROR] CREATOR_DEFINITION_IS_NOT_ALLOWED: 'CREATOR' definition is not allowed. Use 'Parceler' companion object instead.">CREATOR</error> = object : Parcelable.Creator<A> {
            override fun createFromParcel(source: Parcel): A = A("")
            override fun newArray(size: Int) = arrayOfNulls<A>(size)
        }
    }
}