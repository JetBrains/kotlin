// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable
import java.util.Arrays

enum class ParcelableEnum : Parcelable {
    ONE, TWO, THREE;

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(ordinal)
    }

    override fun describeContents() = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ParcelableEnum> = object : Parcelable.Creator<ParcelableEnum> {
            override fun createFromParcel(parcel: Parcel) = ParcelableEnum.ONE
            override fun newArray(size: Int) = arrayOfNulls<ParcelableEnum>(size)
        }
    }
}

@Parcelize
class Test(val parcelableEnum: ParcelableEnum): Parcelable

fun box() = parcelTest { parcel ->
    val first = Test(ParcelableEnum.THREE)

    first.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)

    val first2 = readFromParcel<Test>(parcel)

    assert(first.parcelableEnum == ParcelableEnum.THREE)
    assert(first2.parcelableEnum == ParcelableEnum.ONE)
    assert(first != first2)
}