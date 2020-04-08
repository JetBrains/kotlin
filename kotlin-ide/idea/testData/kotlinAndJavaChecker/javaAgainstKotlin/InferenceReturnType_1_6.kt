@file: JvmName("Util")

package a

interface I

class C : I
class B : I

fun <T : I> T.helper(): T = this