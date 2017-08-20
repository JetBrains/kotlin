package test

import kotlinx.android.parcel.Parcelize
import android.os.Parcelable
import android.os.Parcel

@Parcelize
class A(val a: String) : Parcelable {
    <error descr="[OVERRIDING_WRITE_TO_PARCEL_IS_NOT_ALLOWED] Overriding 'writeToParcel' is not allowed. Use 'Parceler' companion object instead.">override</error> fun writeToParcel(p: Parcel?, flags: Int) {}
    override fun describeContents() = 0
}

@Parcelize
class B(val a: String) : Parcelable {
    <error descr="[OVERRIDING_WRITE_TO_PARCEL_IS_NOT_ALLOWED] Overriding 'writeToParcel' is not allowed. Use 'Parceler' companion object instead.">override</error> fun writeToParcel(p: Parcel?, flags: Int) {}
}

@Parcelize
class C(val a: String) : Parcelable {
    <error descr="[OVERRIDING_WRITE_TO_PARCEL_IS_NOT_ALLOWED] Overriding 'writeToParcel' is not allowed. Use 'Parceler' companion object instead.">override</error> fun writeToParcel(p: Parcel, flags: Int) {}
}