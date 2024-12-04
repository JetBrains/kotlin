// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
class A(_a: String) : Parcelable {
    var a: String = _a
        private set

    companion object : Parceler<A> {
        override fun A.write(parcel: Parcel, flags: Int) {
            parcel.writeString(a)
        }

        override fun create(parcel: Parcel) = A(parcel.readString())
    }
}

fun box() = parcelTest { parcel ->
    val test = A("OK")
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val test2 = parcelableCreator<A>().createFromParcel(parcel)

    assert(test.a == test2.a)
}
