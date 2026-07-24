// CORRECT_ERROR_TYPES

// ISSUE: KT-79655
// Note: currently this test has incorrect expected output (`error.NonExistentClass[].class`).

@file:Suppress("UNRESOLVED_REFERENCE", "ANNOTATION_ARGUMENT_MUST_BE_CONST", "NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION")
package test

import kotlin.reflect.KClass

annotation class Anno(val value: Array<KClass<*>>)

@Anno(value = [Array<Unresolved>::class])
class Klass
