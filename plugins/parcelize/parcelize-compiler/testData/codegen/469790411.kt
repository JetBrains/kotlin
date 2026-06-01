// WITH_STDLIB
// CURIOUS_ABOUT: createFromParcel, writeToParcel

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

class MyClass(val name: String) : Serializable

object MyClassParceler : Parceler<MyClass> {
    override fun create(parcel: Parcel) = TODO()
    override fun MyClass.write(parcel: Parcel, flags: Int) = TODO()
}

@Parcelize
@TypeParceler<MyClass, MyClassParceler>
data class MyParcelableThing1(
    val p: MyClass?
) : Parcelable
