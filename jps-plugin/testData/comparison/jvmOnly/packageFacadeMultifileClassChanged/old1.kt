@file:JvmName("Utils")
@file:JvmMultifileClass
package test

public fun unchangedFun1() {}

private fun removedFun1(): Int = 10

private val removedVal1: String = "A"

private val changedVal1: Int = 20

private fun changedFun1(arg: Int) {}
