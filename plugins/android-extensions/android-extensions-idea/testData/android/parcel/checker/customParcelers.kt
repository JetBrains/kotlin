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

@<error descr="[CLASS_SHOULD_BE_PARCELIZE] Class 'MissingParcelizeAnnotation' should be annotated with '@Parcelize'">TypeParceler</error><String, StringParceler>
class MissingParcelizeAnnotation(val a: @<error descr="[CLASS_SHOULD_BE_PARCELIZE] Class 'MissingParcelizeAnnotation' should be annotated with '@Parcelize'">WriteWith</error><StringParceler> String)

@Parcelize
@TypeParceler<String, StringClassParceler>
class ShouldBeClass(val a: @WriteWith<<error descr="[PARCELER_SHOULD_BE_OBJECT] Parceler should be an object">StringClassParceler</error>> String) : Parcelable

@Parcelize
class Test(
        val a: @WriteWith<<error descr="[PARCELER_TYPE_INCOMPATIBLE] Parceler type String is incompatible with Int">StringParceler</error>> Int,
        val b: @WriteWith<StringParceler> String,
        val c: @WriteWith<<error descr="[PARCELER_TYPE_INCOMPATIBLE] Parceler type String is incompatible with CharSequence">StringParceler</error>> CharSequence,
        val d: @WriteWith<CharSequenceParceler> String,
        val e: @WriteWith<CharSequenceParceler> CharSequence
) : Parcelable

@Parcelize
@TypeParceler<String, StringParceler>
class Test2(@<warning descr="[REDUNDANT_TYPE_PARCELER] This annotation duplicates the one for Class 'Test2'">TypeParceler</warning><String, StringParceler> val a: String) : Parcelable

@Parcelize
@TypeParceler<<error descr="[DUPLICATING_TYPE_PARCELERS] Duplicating ''Parceler'' annotations">String</error>, StringParceler>
@TypeParceler<<error descr="[DUPLICATING_TYPE_PARCELERS] Duplicating ''Parceler'' annotations">String</error>, CharSequenceParceler>
class Test3(val a: String) : Parcelable