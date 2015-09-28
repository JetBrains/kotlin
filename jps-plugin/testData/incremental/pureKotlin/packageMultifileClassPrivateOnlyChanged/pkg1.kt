@file:JvmName("Utils")
@file:JvmMultifileClass
package test

fun commonFun1() {}

// TODO uncomment when generated value will also be private in bytecode
//private val deletedVal1: Int = 20

private fun deletedFun1(): Int = 10

private fun changedFun1(arg: Int) {}