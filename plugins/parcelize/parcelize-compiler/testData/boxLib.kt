package test

import android.os.Parcel

fun parcelTest(block: (Parcel) -> Unit): String {
    val parcel = Parcel.obtain()
    try {
        block(parcel)
        return "OK"
    } finally {
        parcel.recycle()
    }
}
