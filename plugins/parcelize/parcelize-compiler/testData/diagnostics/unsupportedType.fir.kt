package test

import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import android.os.Parcelable

@Parcelize
class User(
        val a: String,
        val b: Any,
        val c: Any?,
        val d: Map<Any, String>,
        val e: @RawValue Any?,
        val f: @RawValue Map<String, Any>,
        val g: Map<String, @RawValue Any>,
        val h: Map<@RawValue Any, List<@RawValue Any>>
) : Parcelable
