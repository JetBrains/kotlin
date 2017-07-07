package test

import kotlinx.android.parcel.MagicParcel
import android.os.Parcelable
import android.os.Parcel

@MagicParcel
class A(val a: String) : Parcelable {
    <error descr="[OVERRIDING_WRITE_TO_PARCEL_IS_NOT_ALLOWED] Overriding 'writeToParcel' is not allowed. Use 'Parceler' companion object instead.">override</error> fun writeToParcel(p: Parcel?, flags: Int) {}
    override fun describeContents() = 0
}

@MagicParcel
class B(val a: String) : Parcelable {
    <error descr="[OVERRIDING_WRITE_TO_PARCEL_IS_NOT_ALLOWED] Overriding 'writeToParcel' is not allowed. Use 'Parceler' companion object instead.">override</error> fun writeToParcel(p: Parcel?, flags: Int) {}
}

@MagicParcel
class C(val a: String) : Parcelable {
    <error descr="[OVERRIDING_WRITE_TO_PARCEL_IS_NOT_ALLOWED] Overriding 'writeToParcel' is not allowed. Use 'Parceler' companion object instead.">override</error> fun writeToParcel(p: Parcel, flags: Int) {}
}