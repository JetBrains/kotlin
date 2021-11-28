package test

import kotlinx.parcelize.*
import android.os.*

class Box(val value: String)

@Parcelize
class Foo(val box: Box): Parcelable {
    companion object : Parceler<Foo> {
        override fun create(parcel: Parcel) = Foo(Box(parcel.readString()))

        override fun Foo.write(parcel: Parcel, flags: Int) {
            parcel.writeString(box.value)
        }
    }
}

@Parcelize
class Foo2(val box: Box): Parcelable
