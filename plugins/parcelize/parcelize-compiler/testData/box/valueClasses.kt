// WITH_RUNTIME
// See: https://issuetracker.google.com/177856519
// IGNORE_BACKEND: JVM

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable
import java.util.UUID

@JvmInline
@Parcelize
value class ParcelableInt(val value: Int) : Parcelable

@JvmInline
@Parcelize
value class ParcelableString(val value: String) : Parcelable

@JvmInline
@Parcelize
value class ParcelableValueClass(val value: ParcelableInt) : Parcelable

@JvmInline
@Parcelize
value class ParcelableNullableValueClass(val value: ParcelableString?) : Parcelable

@Parcelize
data class Data(
    val parcelableInt: ParcelableInt,
    val parcelableString: ParcelableString,
    val parcelableValueClass: ParcelableValueClass,
    val parcelableNullableValueClass: ParcelableNullableValueClass,
    val parcelableNullableValueClassNullable: ParcelableNullableValueClass?,
) : Parcelable

fun box() = parcelTest { parcel ->
    val data = Data(
        ParcelableInt(10),
        ParcelableString(""),
        ParcelableValueClass(ParcelableInt(30)),
        ParcelableNullableValueClass(null),
        null,
    )
    data.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val data2 = readFromParcel<Data>(parcel)
    assert(data2 == data)
}
