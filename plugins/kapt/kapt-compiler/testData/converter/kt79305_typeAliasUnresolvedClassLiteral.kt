// CORRECT_ERROR_TYPES
// WITH_STDLIB

@file:Suppress("UNRESOLVED_REFERENCE", "ANNOTATION_ARGUMENT_MUST_BE_CONST")
import kotlin.reflect.KClass

typealias T = Unresolved

@Anno(T::class)
class C

annotation class Anno(val value: KClass<*>)
