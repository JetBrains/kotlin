annotation class Annotation

fun foo(@Annotation arg: Int) {}

// KT-56018 TODO uncomment the line below to see failure on K2/N
//data class Clazz(@Annotation val param: Int)
