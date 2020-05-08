// This issue affects AIDL generated files, as reported in KT-25807
// WITH_RUNTIME
// FILE: J.java
import android.os.Parcel;
import test.K;

public class J {
    public static K readParcel(Parcel parcel) {
        return K.CREATOR.createFromParcel(parcel);
    }
}

// FILE: test.kt
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
data class K(val x: Int) : Parcelable

fun box() = parcelTest { parcel ->
    val first = K(0)
    first.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)

    val second = J.readParcel(parcel)
    assert(first == second)
}
