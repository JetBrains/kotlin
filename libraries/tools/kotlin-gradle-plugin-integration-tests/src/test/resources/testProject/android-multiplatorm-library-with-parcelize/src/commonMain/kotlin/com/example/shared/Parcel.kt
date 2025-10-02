package com.example.shared

expect class Parcel {
    fun writeLong(long: Long)
    fun readLong(): Long
}

expect interface Parcelable
