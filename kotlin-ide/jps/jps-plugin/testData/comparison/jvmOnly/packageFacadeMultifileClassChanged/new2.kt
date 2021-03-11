@file:JvmName("Utils")
@file:JvmMultifileClass
package test

public fun unchangedFun2() {}

private fun addedFun2(): Int = 10

private val addedVal2: String = "A"

private val changedVal2: String = ""

private fun changedFun2(arg: String) {}
