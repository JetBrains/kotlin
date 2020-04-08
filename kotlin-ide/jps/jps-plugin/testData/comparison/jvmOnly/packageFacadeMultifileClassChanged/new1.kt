@file:JvmName("Utils")
@file:JvmMultifileClass
package test

public fun unchangedFun1() {}

public fun publicAddedFun1() {}

private fun addedFun1(): Int = 10

private val addedVal1: String = "A"

private val changedVal1: String = ""

private fun changedFun1(arg: String) {}
