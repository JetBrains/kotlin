@file:JvmName("Utils")
@file:JvmMultifileClass
package test

public fun unchangedFun2() {}

private fun removedFun2(): Int = 10

private val removedVal2: String = "A"

private val changedVal2: Int = 20

private fun changedFun2(arg: Int) {}
