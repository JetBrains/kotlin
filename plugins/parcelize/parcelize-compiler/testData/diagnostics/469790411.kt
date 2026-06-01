// WITH_STDLIB

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

open class MyClass(val name: String)
class MySubClass() : MyClass("")

object MyClassParceler : Parceler<MyClass> {
    override fun create(parcel: Parcel) = TODO()
    override fun MyClass.write(parcel: Parcel, flags: Int) = TODO()
}

@Parcelize
@TypeParceler<MyClass, MyClassParceler>
data class MyParcelableThing1(
    val p1: MyClass,
    val p2: <!PARCELABLE_TYPE_NOT_SUPPORTED!>MyClass?<!>,
) : Parcelable

@Parcelize
data class MyParcelableThing2(
    val p1: @WriteWith<MyClassParceler> MyClass,
    val p2: @WriteWith<<!PARCELER_TYPE_INCOMPATIBLE!>MyClassParceler<!>> MyClass?,
    val p3: @WriteWith<MyClassParceler> MySubClass,
    val p4: @WriteWith<<!PARCELER_TYPE_INCOMPATIBLE!>MyClassParceler<!>> MySubClass?,
) : Parcelable
