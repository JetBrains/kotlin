package com.example.shared

actual annotation class Parcelize
actual annotation class TypeParceler<T, P : Parceler<in T>>
actual interface Parceler<T> {
    actual fun create(parcel: Parcel): T
    actual fun T.write(parcel: Parcel, flags: Int)
}
actual annotation class IgnoredOnParcel
