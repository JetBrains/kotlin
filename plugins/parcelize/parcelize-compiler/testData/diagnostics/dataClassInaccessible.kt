// https://issuetracker.google.com/TODO
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// RENDER_DIAGNOSTICS_FULL_TEXT
@file:OptIn(kotlinx.parcelize.Experimental::class)

import android.os.Parcelable
import kotlinx.parcelize.DataClass
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

data class Secret(private val x: Int)

@Parcelize
class P(
    val secret: <!PARCELABLE_TYPE_NOT_SUPPORTED!>@DataClass Secret<!>,
    val rawSecret: <!PARCELABLE_TYPE_CONTAINS_NOT_SUPPORTED!>@RawValue @DataClass Secret<!>,
) : Parcelable
