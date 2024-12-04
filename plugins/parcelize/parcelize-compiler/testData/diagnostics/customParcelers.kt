// FIR_IDENTICAL
// WITH_STDLIB
package test

import kotlinx.parcelize.*
import android.os.*

class Data<T>
typealias StringData = Data<String>
typealias NullableStringData = Data<String>?

object StringParceler : Parceler<String> {
    override fun create(parcel: Parcel) = TODO()
    override fun String.write(parcel: Parcel, flags: Int) = TODO()
}

object CharSequenceParceler : Parceler<CharSequence> {
    override fun create(parcel: Parcel) = TODO()
    override fun CharSequence.write(parcel: Parcel, flags: Int) = TODO()
}

typealias CharSequenceParcelerAlias = CharSequenceParceler

class StringClassParceler : Parceler<String> {
    override fun create(parcel: Parcel) = TODO()
    override fun String.write(parcel: Parcel, flags: Int) = TODO()
}

class StringDataParceler : Parceler<StringData> {
    override fun create(parcel: Parcel) = TODO()
    override fun StringData.write(parcel: Parcel, flags: Int) = TODO()
}

class NullableStringDataParceler : Parceler<NullableStringData> {
    override fun create(parcel: Parcel) = TODO()
    override fun NullableStringData.write(parcel: Parcel, flags: Int) = TODO()
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
    val e: @WriteWith<CharSequenceParceler> CharSequence,
    val f: @WriteWith<CharSequenceParcelerAlias> String,
    val g: @WriteWith<CharSequenceParcelerAlias> CharSequence,
) : Parcelable

@Parcelize
@TypeParceler<String, StringParceler>
class Test2(@<!REDUNDANT_TYPE_PARCELER!>TypeParceler<!><String, StringParceler> val a: String) : Parcelable

@Parcelize
@TypeParceler<<!DUPLICATING_TYPE_PARCELERS!>String<!>, StringParceler>
@TypeParceler<<!DUPLICATING_TYPE_PARCELERS!>String<!>, CharSequenceParceler>
class Test3(val a: String) : Parcelable

@Parcelize
@TypeParceler<StringData, StringDataParceler>
class StringDataParcelerTest(
    val a: StringData,
    val b: <!PARCELABLE_TYPE_NOT_SUPPORTED!>StringData?<!>,
    val c: Data<String>,
    val d: <!PARCELABLE_TYPE_NOT_SUPPORTED!>Data<String>?<!>,
) : Parcelable

@Parcelize
@TypeParceler<NullableStringData, NullableStringDataParceler>
class NullableStringDataParcelerTest(
    val a: NullableStringData,
    val b: <!PARCELABLE_TYPE_NOT_SUPPORTED!>StringData<!>,
    val c: StringData?,
    val d: <!PARCELABLE_TYPE_NOT_SUPPORTED!>Data<String><!>,
    val e: Data<String>?,
) : Parcelable
