@file:JvmName("Utils")
@file:JvmMultifileClass
package test

fun commonFun2() {}

fun publicDeletedFun2() {}

// TODO uncomment when generated value will also be private in bytecode
//private val deletedVal2: Int = 20

private fun deletedFun2(): Int = 10

private fun changedFun2(arg: Int) {}