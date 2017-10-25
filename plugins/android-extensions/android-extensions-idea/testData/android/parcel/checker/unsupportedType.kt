package test

import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import android.os.Parcelable

@Parcelize
class User(
        val a: String,
        val b: <error descr="[PLUGIN_ERROR] Type is not directly supported by 'Parcelize'. Annotate the parameter type with '@RawValue' if you want it to be serialized using 'writeValue()'">Any</error>,
        val c: <error descr="[PLUGIN_ERROR] Type is not directly supported by 'Parcelize'. Annotate the parameter type with '@RawValue' if you want it to be serialized using 'writeValue()'">Any?</error>,
        val d: <error descr="[PLUGIN_ERROR] Type is not directly supported by 'Parcelize'. Annotate the parameter type with '@RawValue' if you want it to be serialized using 'writeValue()'">Map<Any, String></error>,
        val e: @RawValue Any?,
        val f: @RawValue Map<String, Any>,
        val g: Map<String, @RawValue Any>,
        val h: Map<@RawValue Any, List<@RawValue Any>>
) : Parcelable
