// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

class T(val value: String)

// There's no warnings on empty constructors, secondary constructors, or
// non-parcelable types if there is a custom parceler.
@Parcelize
class A() : Parcelable {
    var a: T = T("Fail")

    constructor(value: String) : this() {
        a = T(value)
    }

    companion object : Parceler<A> {
        override fun A.write(parcel: Parcel, flags: Int) {
            parcel.writeString(a.value)
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

    assert(test.a.value == test2.a.value)
}
