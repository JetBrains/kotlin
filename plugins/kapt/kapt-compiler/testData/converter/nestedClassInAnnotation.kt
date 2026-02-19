@file:Suppress("UNRESOLVED_REFERENCE", "ANNOTATION_ARGUMENT_MUST_BE_CONST", "NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION")
package test

import kotlin.reflect.KClass

annotation class Anno(val value: Array<KClass<*>>)

@Anno(value = [
    A::class,
    A.B::class,
    A.B.C::class,
])
class Klass {
    private lateinit var x: A.B
}
