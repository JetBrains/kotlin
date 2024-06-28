// FIR_IDENTICAL

package test

import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import android.os.Parcelable

typealias TypeAlias = Any
typealias RawValueTypeAlias = @RawValue Any

@Parcelize
class User(
        val a: String,
        val b: <!PARCELABLE_TYPE_NOT_SUPPORTED!>Any<!>,
        val c: <!PARCELABLE_TYPE_NOT_SUPPORTED!>Any?<!>,
        val d: <!PARCELABLE_TYPE_NOT_SUPPORTED!>Map<Any, String><!>,
        val e: @RawValue Any?,
        val f: @RawValue Map<String, Any>,
        val g: Map<String, @RawValue Any>,
        val h: Map<@RawValue Any, List<@RawValue Any>>,
        val i: @RawValue TypeAlias,
        val j: RawValueTypeAlias,
) : Parcelable
