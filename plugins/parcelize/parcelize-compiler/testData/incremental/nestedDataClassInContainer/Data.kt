@file:OptIn(kotlinx.parcelize.Experimental::class)

package test

import kotlinx.parcelize.DataClass

data class Data(val items: List<@DataClass Inner>)
