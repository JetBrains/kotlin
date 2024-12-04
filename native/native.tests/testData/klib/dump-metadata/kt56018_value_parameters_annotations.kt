// FIR_IDENTICAL
package test
annotation class Annotation

fun foo(@Annotation arg: Int) {}

// KT-56177 TODO uncomment the line after KT-56177 is fixed
//data class Clazz(@Annotation val param: Int)
