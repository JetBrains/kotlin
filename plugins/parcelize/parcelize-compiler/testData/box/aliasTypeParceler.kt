// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

data class MyString(val string: String)

object MyStringParceler: Parceler<MyString> {
    override fun MyString.write(parcel: Parcel, flags: Int) {
        parcel.writeString(this.string)
    }
    override fun create(parcel: Parcel): MyString {
        return MyString(parcel.readString()!!)
    }
}

typealias MyStringTypeParceler = TypeParceler<MyString, MyStringParceler>

@Parcelize
data class Test(
    @MyStringTypeParceler val data: MyString
): Parcelable


fun box() = parcelTest { parcel ->
    val want = Test(
        data = MyString("hello")
    )

    want.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val got = parcelableCreator<Test>().createFromParcel(parcel)

    assert(want == got)
}
