// WITH_STDLIB
// See: https://issuetracker.google.com/197890119
// IGNORE_BACKEND: JVM

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
@JvmInline
value class ListWrapper(val list: List<String>) : Parcelable

@Parcelize
data class Wrapper(val listWrapper: ListWrapper) : Parcelable

@Parcelize
data class NullableWrapper(val listWrapper: ListWrapper?) : Parcelable

fun box() = parcelTest { parcel ->
    val data = Wrapper(ListWrapper(listOf("O", "K")))
    val none = NullableWrapper(null)
    data.writeToParcel(parcel, 0)
    none.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val data2 = parcelableCreator<Wrapper>().createFromParcel(parcel)
    assert(data2 == data)

    val none2 = parcelableCreator<NullableWrapper>().createFromParcel(parcel)
    assert(none2 == none)
}
