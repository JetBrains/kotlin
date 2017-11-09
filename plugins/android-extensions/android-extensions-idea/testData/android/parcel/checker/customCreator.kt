package test

import kotlinx.android.parcel.Parcelize
import android.os.Parcelable
import android.os.Parcel

@Parcelize
class A(val a: String) : Parcelable {
    companion object {
        @JvmField
        val <error descr="[PLUGIN_ERROR] 'CREATOR' definition is not allowed. Use 'Parceler' companion object instead">CREATOR</error> = object : Parcelable.Creator<A> {
            override fun createFromParcel(source: Parcel): A = A("")
            override fun newArray(size: Int) = arrayOfNulls<A>(size)
        }
    }
}

@Parcelize
class B(val b: String) : Parcelable {
    companion object <error descr="[PLUGIN_ERROR] 'CREATOR' definition is not allowed. Use 'Parceler' companion object instead">CREATOR</error> : Parcelable.Creator<B> {
        override fun createFromParcel(source: Parcel): B = B("")
        override fun newArray(size: Int) = arrayOfNulls<B>(size)
    }
}
