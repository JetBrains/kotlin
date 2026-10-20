// https://issuetracker.google.com/575732249
// WITH_STDLIB
@file:OptIn(kotlinx.parcelize.Experimental::class)

package test

import kotlinx.parcelize.*
import android.os.Parcelable

data class Node(val x: Int, val next: Node?)

@Parcelize
class MyParcelableThing(val n: @DataClass Node) : Parcelable
