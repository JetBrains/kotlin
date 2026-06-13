// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

class MyClass(val name: String)

object MyClassParceler : Parceler<MyClass> {
    override fun create(parcel: Parcel) = MyClass(parcel.readString()!!)
    override fun MyClass.write(parcel: Parcel, flags: Int) = parcel.writeString(name)
}

@Parcelize
@TypeParceler<MyClass, MyClassParceler>
data class MyParcelableThing1(
    val p: MyClass?
) : Parcelable

@Parcelize
data class MyParcelableThing2(
    val p: @WriteWith<MyClassParceler> MyClass?
) : Parcelable

fun box() = parcelTest { parcel ->
    val thing1NonNull = MyParcelableThing1(MyClass("A"))
    val thing1Null = MyParcelableThing1(null)

    val thing2NonNull = MyParcelableThing2(MyClass("B"))
    val thing2Null = MyParcelableThing2(null)

    thing1NonNull.writeToParcel(parcel, 0)
    thing1Null.writeToParcel(parcel, 0)
    thing2NonNull.writeToParcel(parcel, 0)
    thing2Null.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val thing1NonNull2 = parcelableCreator<MyParcelableThing1>().createFromParcel(parcel)
    assert(thing1NonNull2.p?.name == "A")

    val thing1Null2 = parcelableCreator<MyParcelableThing1>().createFromParcel(parcel)
    assert(thing1Null2.p == null)

    val thing2NonNull2 = parcelableCreator<MyParcelableThing2>().createFromParcel(parcel)
    assert(thing2NonNull2.p?.name == "B")

    val thing2Null2 = parcelableCreator<MyParcelableThing2>().createFromParcel(parcel)
    assert(thing2Null2.p == null)

}
