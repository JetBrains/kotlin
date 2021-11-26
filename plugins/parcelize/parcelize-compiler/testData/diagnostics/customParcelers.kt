// WITH_STDLIB
package test

import kotlinx.parcelize.*
import android.os.*

object StringParceler : Parceler<String> {
    override fun create(parcel: Parcel) = TODO()
    override fun String.write(parcel: Parcel, flags: Int) = TODO()
}

object CharSequenceParceler : Parceler<CharSequence> {
    override fun create(parcel: Parcel) = TODO()
    override fun CharSequence.write(parcel: Parcel, flags: Int) = TODO()
}

class StringClassParceler : Parceler<String> {
    override fun create(parcel: Parcel) = TODO()
    override fun String.write(parcel: Parcel, flags: Int) = TODO()
}

@<!CLASS_SHOULD_BE_PARCELIZE!>TypeParceler<!><String, StringParceler>
class MissingParcelizeAnnotation(val a: @<!CLASS_SHOULD_BE_PARCELIZE!>WriteWith<!><StringParceler> String)

@Parcelize
@TypeParceler<String, StringClassParceler>
class ShouldBeClass(val a: @WriteWith<<!PARCELER_SHOULD_BE_OBJECT!>StringClassParceler<!>> String) : Parcelable

@Parcelize
class Test(
    val a: @WriteWith<<!PARCELER_TYPE_INCOMPATIBLE!>StringParceler<!>> Int,
    val b: @WriteWith<StringParceler> String,
    val c: @WriteWith<<!PARCELER_TYPE_INCOMPATIBLE!>StringParceler<!>> CharSequence,
    val d: @WriteWith<CharSequenceParceler> String,
    val e: @WriteWith<CharSequenceParceler> CharSequence
) : Parcelable

@Parcelize
@TypeParceler<String, StringParceler>
class Test2(@<!REDUNDANT_TYPE_PARCELER!>TypeParceler<!><String, StringParceler> val a: String) : Parcelable

@Parcelize
@TypeParceler<<!DUPLICATING_TYPE_PARCELERS!>String<!>, StringParceler>
@TypeParceler<<!DUPLICATING_TYPE_PARCELERS!>String<!>, CharSequenceParceler>
class Test3(val a: String) : Parcelable
