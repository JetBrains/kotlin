@file:OptIn(kotlinx.parcelize.Experimental::class)

package test

import kotlinx.parcelize.DataClass

data class Data(val i: Int, val s: String)

typealias DataAlias = @DataClass Data
