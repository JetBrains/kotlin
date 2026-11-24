// https://issuetracker.google.com/590046666
// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
@file:OptIn(kotlinx.parcelize.Experimental::class)

import android.os.Parcelable
import kotlinx.parcelize.DataClass
import kotlinx.parcelize.Parcelize

data class Foo(val x: Int) {
    val String.x: Int get() = length
}

@Parcelize
class P(val foo: @DataClass Foo) : Parcelable
