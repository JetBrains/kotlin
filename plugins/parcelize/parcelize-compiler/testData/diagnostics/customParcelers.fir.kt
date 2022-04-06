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

@TypeParceler<String, StringParceler>
class MissingParcelizeAnnotation(val a: @WriteWith<StringParceler> String)

@Parcelize
@TypeParceler<String, StringClassParceler>
class ShouldBeClass(val a: @WriteWith<StringClassParceler> String) : Parcelable

@Parcelize
class Test(
    val a: @WriteWith<StringParceler> Int,
    val b: @WriteWith<StringParceler> String,
    val c: @WriteWith<StringParceler> CharSequence,
    val d: @WriteWith<CharSequenceParceler> String,
    val e: @WriteWith<CharSequenceParceler> CharSequence
) : Parcelable

@Parcelize
@TypeParceler<String, StringParceler>
class Test2(@TypeParceler<String, StringParceler> val a: String) : Parcelable

@Parcelize
@TypeParceler<String, StringParceler>
@TypeParceler<String, CharSequenceParceler>
class Test3(val a: String) : Parcelable
