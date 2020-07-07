package test

import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import android.os.Parcelable

@Parcelize
class User(
        val a: String,
        val b: <error descr="[PARCELABLE_TYPE_NOT_SUPPORTED] Type is not directly supported by 'Parcelize'. Annotate the parameter type with '@RawValue' if you want it to be serialized using 'writeValue()'">Any</error>,
        val c: <error descr="[PARCELABLE_TYPE_NOT_SUPPORTED] Type is not directly supported by 'Parcelize'. Annotate the parameter type with '@RawValue' if you want it to be serialized using 'writeValue()'">Any?</error>,
        val d: <error descr="[PARCELABLE_TYPE_NOT_SUPPORTED] Type is not directly supported by 'Parcelize'. Annotate the parameter type with '@RawValue' if you want it to be serialized using 'writeValue()'">Map<Any, String></error>,
        val e: @RawValue Any?,
        val f: @RawValue Map<String, Any>,
        val g: Map<String, @RawValue Any>,
        val h: Map<@RawValue Any, List<@RawValue Any>>
) : Parcelable
