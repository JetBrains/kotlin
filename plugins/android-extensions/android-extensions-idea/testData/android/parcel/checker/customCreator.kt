package test

import kotlinx.android.parcel.MagicParcel
import android.os.Parcelable
import android.os.Parcel

@MagicParcel
class A(val a: String) : Parcelable {
    companion object {
        @JvmField
        val <error descr="[CREATOR_DEFINITION_IS_FORBIDDEN] 'CREATOR' definition is forbidden. Use 'Parceler' nested object instead.">CREATOR</error> = object : Parcelable.Creator<A> {
            override fun createFromParcel(source: Parcel): A = A("")
            override fun newArray(size: Int) = arrayOfNulls<A>(size)
        }
    }
}