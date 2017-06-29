package test

import kotlinx.android.parcel.MagicParcel
import android.os.Parcelable
import android.os.Parcel

@MagicParcel
class A(val a: String) : Parcelable {
    <error descr="[OVERRIDING_WRITE_TO_PARCEL_IS_FORBIDDEN] Overriding 'writeToParcel' is forbidden. Use 'Parceler' nested object instead.">override</error> fun writeToParcel(p: Parcel?, flags: Int) {}
    override fun describeContents() = 0
}

@MagicParcel
class B(val a: String) : Parcelable {
    <error descr="[OVERRIDING_WRITE_TO_PARCEL_IS_FORBIDDEN] Overriding 'writeToParcel' is forbidden. Use 'Parceler' nested object instead.">override</error> fun writeToParcel(p: Parcel?, flags: Int) {}
}

@MagicParcel
class C(val a: String) : Parcelable {
    <error descr="[OVERRIDING_WRITE_TO_PARCEL_IS_FORBIDDEN] Overriding 'writeToParcel' is forbidden. Use 'Parceler' nested object instead.">override</error> fun writeToParcel(p: Parcel, flags: Int) {}
}