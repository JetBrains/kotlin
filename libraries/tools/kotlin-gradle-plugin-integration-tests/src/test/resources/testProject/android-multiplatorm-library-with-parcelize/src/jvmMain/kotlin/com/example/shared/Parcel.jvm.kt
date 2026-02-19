package com.example.shared

actual class Parcel {
    actual fun writeLong(long: Long): Unit = TODO()
    actual fun readLong(): Long = TODO()
}
actual interface Parcelable
