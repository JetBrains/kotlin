package android.os

class Parcel

interface Parcelable {
    fun describeContents(): Int

    fun writeToParcel(parcel: Parcel, flags: Int)

    interface Creator<T> {
        fun createFromParcel(parcel: Parcel): T
        fun newArray(size: Int): Array<T>
    }
}