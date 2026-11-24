// https://issuetracker.google.com/TODO
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
@file:OptIn(kotlinx.parcelize.Experimental::class)

import android.os.Parcelable
import kotlinx.parcelize.DataClass
import kotlinx.parcelize.Parcelize

data class NonNullBox<T>(val x: T & Any)

@Parcelize
class P(val b: <!PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED!>@DataClass NonNullBox<NonNullBox<Int>><!>) : Parcelable
