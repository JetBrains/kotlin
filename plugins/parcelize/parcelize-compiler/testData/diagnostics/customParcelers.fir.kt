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

@<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>TypeParceler<!><String, StringParceler>
class MissingParcelizeAnnotation(val a: @<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>WriteWith<!><StringParceler> String)

@Parcelize
@<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>TypeParceler<!><String, StringClassParceler>
class ShouldBeClass(val a: @<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>WriteWith<!><StringClassParceler> String) : Parcelable

@Parcelize
class Test(
    val a: @<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>WriteWith<!><StringParceler> Int,
    val b: @<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>WriteWith<!><StringParceler> String,
    val c: @<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>WriteWith<!><StringParceler> CharSequence,
    val d: @<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>WriteWith<!><CharSequenceParceler> String,
    val e: @<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>WriteWith<!><CharSequenceParceler> CharSequence
) : Parcelable

@Parcelize
@<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>TypeParceler<!><String, StringParceler>
class Test2(@<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>TypeParceler<!><String, StringParceler> val a: String) : Parcelable

@Parcelize
@<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>TypeParceler<!><String, StringParceler>
@<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>TypeParceler<!><String, CharSequenceParceler>
class Test3(val a: String) : Parcelable
