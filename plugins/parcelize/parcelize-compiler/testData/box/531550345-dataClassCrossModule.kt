// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt

package test

data class Data(val i: Int, val s: String)
data class NullableData(val i: Int?, val s: String?)
data class DefaultData(val i: Int = 42, val s: String? = "default", val n: String? = null)
data class NestedData(val data: Data)

// MODULE: main(lib)
// FILE: main.kt

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
class DataWrapper @OptIn(Experimental::class) constructor(val data: @DataClass Data) : Parcelable

@Parcelize
class NullableWrapper @OptIn(Experimental::class) constructor(val data: @DataClass NullableData) : Parcelable

@Parcelize
class DefaultWrapper @OptIn(Experimental::class) constructor(val data: @DataClass DefaultData) : Parcelable

@Parcelize
class NestedWrapper @OptIn(Experimental::class) constructor(val data: @DataClass NestedData) : Parcelable

fun box() = parcelTest { parcel ->
    val vData = DataWrapper(Data(42, ""))
    val vNull = NullableWrapper(NullableData(null, "not null"))
    val vDef = DefaultWrapper(DefaultData())
    val vNested = NestedWrapper(NestedData(Data(42, "nested")))

    vData.writeToParcel(parcel, 0)
    vNull.writeToParcel(parcel, 0)
    vDef.writeToParcel(parcel, 0)
    vNested.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val vData2 = parcelableCreator<DataWrapper>().createFromParcel(parcel)
    val vNull2 = parcelableCreator<NullableWrapper>().createFromParcel(parcel)
    val vDef2 = parcelableCreator<DefaultWrapper>().createFromParcel(parcel)
    val vNested2 = parcelableCreator<NestedWrapper>().createFromParcel(parcel)

    assert(vData.data == vData2.data) { "Data: expected ${vData.data}, got ${vData2.data}" }
    assert(vNull.data == vNull2.data) { "Nullable: expected ${vNull.data}, got ${vNull2.data}" }
    assert(vDef.data == vDef2.data) { "Default: expected ${vDef.data}, got ${vDef2.data}" }
    assert(vNested.data == vNested2.data) { "Nested: expected ${vNested.data}, got ${vNested2.data}" }
}

