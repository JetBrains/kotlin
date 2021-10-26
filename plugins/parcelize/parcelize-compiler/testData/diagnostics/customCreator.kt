// FIR_IDENTICAL
// WITH_STDLIB
package test

import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import android.os.Parcel

@Parcelize
class A(val a: String) : Parcelable {
    companion object {
        @JvmField
        val <!CREATOR_DEFINITION_IS_NOT_ALLOWED!>CREATOR<!> = object : Parcelable.Creator<A> {
            override fun createFromParcel(source: Parcel): A = A("")
            override fun newArray(size: Int) = arrayOfNulls<A>(size)
        }
    }
}

@Parcelize
class B(val b: String) : Parcelable {
    companion object <!CREATOR_DEFINITION_IS_NOT_ALLOWED!>CREATOR<!> : Parcelable.Creator<B> {
        override fun createFromParcel(source: Parcel): B = B("")
        override fun newArray(size: Int) = arrayOfNulls<B>(size)
    }
}
