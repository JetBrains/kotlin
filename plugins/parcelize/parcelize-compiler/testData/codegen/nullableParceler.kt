// WITH_STDLIB
// CURIOUS_ABOUT: createFromParcel, writeToParcel

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

class MyClass(val name: String) : Serializable

object MyClassParceler : Parceler<MyClass> {
    override fun create(parcel: Parcel) = MyClass("")
    override fun MyClass.write(parcel: Parcel, flags: Int) = Unit
}

object MyNullableClassParceler : Parceler<MyClass?> {
    override fun create(parcel: Parcel) = null
    override fun MyClass?.write(parcel: Parcel, flags: Int) = Unit
}

@Parcelize
@TypeParceler<MyClass, MyClassParceler>
@TypeParceler<MyClass?, MyNullableClassParceler>
data class MyParcelableThing1(
    val p2: MyClass?,
    val p1: MyClass,
) : Parcelable
