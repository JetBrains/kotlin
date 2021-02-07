package test

import kotlin.reflect.KClass

annotation class Anno(
    val klass: KClass<*>,
    val klasses: Array<KClass<*>>,
    val sarKlass: KClass<Array<String>>,
    val d2arKlass: KClass<Array<DoubleArray>>
)

@Anno(
    String::class,
    arrayOf(Int::class, String::class, Float::class),
    Array<String>::class,
    Array<DoubleArray>::class
)
class Klass
