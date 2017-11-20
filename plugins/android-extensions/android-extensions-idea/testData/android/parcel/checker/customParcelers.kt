package test

import kotlinx.android.parcel.*
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

@<error descr="[PLUGIN_ERROR] Class 'MissingParcelizeAnnotation' should be annotated with '@Parcelize'">TypeParceler</error><String, StringParceler>
class MissingParcelizeAnnotation(val a: @<error descr="[PLUGIN_ERROR] Class 'MissingParcelizeAnnotation' should be annotated with '@Parcelize'">WriteWith</error><StringParceler> String)

@Parcelize
@TypeParceler<String, StringClassParceler>
class ShouldBeClass(val a: @WriteWith<<error descr="[PLUGIN_ERROR] Parceler should be an object">StringClassParceler</error>> String) : Parcelable

@Parcelize
class Test(
        val a: @WriteWith<<error descr="[PLUGIN_ERROR] Parceler type String is incompatible with Int">StringParceler</error>> Int,
        val b: @WriteWith<StringParceler> String,
        val c: @WriteWith<<error descr="[PLUGIN_ERROR] Parceler type String is incompatible with CharSequence">StringParceler</error>> CharSequence,
        val d: @WriteWith<CharSequenceParceler> String,
        val e: @WriteWith<CharSequenceParceler> CharSequence
) : Parcelable

@Parcelize
@TypeParceler<String, StringParceler>
class Test2(@<warning descr="[PLUGIN_WARNING] This 'TypeParceler' is already provided for Class 'Test2'">TypeParceler</warning><String, StringParceler> val a: String) : Parcelable

@Parcelize
@TypeParceler<<error descr="[PLUGIN_ERROR] Duplicating ''TypeParceler'' annotations">String</error>, StringParceler>
@TypeParceler<<error descr="[PLUGIN_ERROR] Duplicating ''TypeParceler'' annotations">String</error>, CharSequenceParceler>
class Test3(val a: String) : Parcelable
