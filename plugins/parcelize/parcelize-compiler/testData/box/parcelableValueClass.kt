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
value class ParcelableUuid(val uuid: UUID) : Parcelable {
    override fun describeContents(): Int = 0
    override fun writeToParcel(parcel: Parcel, flags: Int) = parcel.writeString(uuid.toString())
    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<ParcelableUuid> {
            override fun createFromParcel(source: Parcel): ParcelableUuid = ParcelableUuid(UUID.fromString(source.readString()))
            override fun newArray(size: Int): Array<ParcelableUuid?> = arrayOfNulls(size)
        }
    }
}

@Parcelize
class Data(val uuid: ParcelableUuid) : Parcelable

fun box() = parcelTest { parcel ->
    val data = Data(ParcelableUuid(UUID.randomUUID()))
    data.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val data2 = readFromParcel<Data>(parcel)
    assert(data2.uuid.uuid == data.uuid.uuid)
}
