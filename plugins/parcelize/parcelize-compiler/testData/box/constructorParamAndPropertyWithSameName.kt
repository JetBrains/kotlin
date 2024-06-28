// WITH_STDLIB
// Bug: https://issuetracker.google.com/issues/345674552
@file:JvmName("TestKt")

package test


import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
class Foo(foo: String) : Parcelable {
    val igore = foo
    val foo: String
        get() = "dummy"

    companion object : Parceler<Foo> {
        override fun create(parcel: Parcel) = Foo(parcel.readString())
        override fun Foo.write(parcel: Parcel, flags: Int) {
            parcel.writeString(foo)
        }
    }
}

fun box() = parcelTest { parcel ->
    val foo = Foo("hello")
    foo.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)
    val foo2 = parcelableCreator<Foo>().createFromParcel(parcel)
    assert(foo2.foo == foo.foo)
}
