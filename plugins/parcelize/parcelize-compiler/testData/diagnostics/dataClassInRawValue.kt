// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// RENDER_DIAGNOSTICS_FULL_TEXT
@file:OptIn(kotlinx.parcelize.Experimental::class)

package test

import kotlinx.parcelize.*
import android.os.Parcelable

// Data classes in a `@RawValue` type are still serialized property by property, so `@RawValue` does not relax their requirements.
// This used to crash the backend: `PrivateNode` was rejected because of its private property before its properties were checked
// for recursion, and that rejection was dropped inside `@RawValue`, while the backend still serialized it inline.
data class PrivateNode(private val next: PrivateNode?)

@Parcelize
class Test(val node: <!PARCELABLE_TYPE_CONTAINS_NOT_SUPPORTED!>@RawValue @DataClass PrivateNode<!>) : Parcelable

data class Accepted(val x: String)
data class UnsupportedField(val x: Any)
data class PrivateField(private val x: String)
data class ContainsRawValuePrivateField(val field: @RawValue PrivateField)
data class Node(val next: Node?)
data class RawValueNode(val next: @RawValue RawValueNode?)

@Parcelize
class C(
    val privateField: <!PARCELABLE_TYPE_CONTAINS_NOT_SUPPORTED!>@RawValue @DataClass PrivateField<!>,
    val privateFieldInContainer: <!PARCELABLE_TYPE_CONTAINS_NOT_SUPPORTED!>@RawValue List<@DataClass PrivateField><!>,
    val privateFieldInDataClass: <!PARCELABLE_TYPE_CONTAINS_NOT_SUPPORTED!>@DataClass ContainsRawValuePrivateField<!>,

    // Recursive data classes in a `@RawValue` type are still reported.
    val recursive: <!PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED!>@RawValue @DataClass Node<!>,
    val recursiveInContainer: <!PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED!>@RawValue List<@DataClass Node><!>,
    val recursiveThroughRawValue: <!PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED!>@DataClass RawValueNode<!>,

    // Unsupported types in data classes in a `@RawValue` type are serialized with `writeValue`.
    val accepted: @RawValue @DataClass Accepted,
    val unsupportedField: @RawValue @DataClass UnsupportedField,
    val unsupportedFieldInContainer: @RawValue List<@DataClass UnsupportedField>,

    // Data classes which are not serialized property by property are serialized with `writeValue`.
    val notInline: @RawValue PrivateField,
) : Parcelable
