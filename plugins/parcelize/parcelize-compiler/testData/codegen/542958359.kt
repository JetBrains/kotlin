// WITH_STDLIB
// CURIOUS_ABOUT: createFromParcel, writeToParcel

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

class MyClass()

interface MyIntermediateParceler<T> : Parceler<T>

object MyClassParceler : MyIntermediateParceler<MyClass?> {
    override fun create(parcel: Parcel): MyClass? = null
    override fun MyClass?.write(parcel: Parcel, flags: Int) {}
}

object MyNonNullableClassParceler : MyIntermediateParceler<MyClass> {
    override fun create(parcel: Parcel): MyClass = TODO()
    override fun MyClass.write(parcel: Parcel, flags: Int) {}
}


@Parcelize
data class MyParcelable(
    val myProperty1: @WriteWith<MyClassParceler> MyClass? = null,
    val myProperty2: @WriteWith<MyNonNullableClassParceler> MyClass? = null,
) : Parcelable
