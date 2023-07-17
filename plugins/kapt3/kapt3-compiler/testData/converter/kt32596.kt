// FIR_BLOCKED: KT-60482
// CORRECT_ERROR_TYPES

@file:Suppress("UNRESOLVED_REFERENCE", "ANNOTATION_ARGUMENT_MUST_BE_CONST")
import kotlin.reflect.KClass

@ABC
class ErrorMissingAnnotation

@ABC @CDE
class ErrorMultipleMissingAnnotations

@CDE @Anno(ABC::class) @ABC
class ErrorSomeMissingAnnotations

annotation class Anno(val klass: KClass<*>)

// EXPECTED_ERROR: (kotlin:10:1) cannot find symbol
// EXPECTED_ERROR: (kotlin:13:1) cannot find symbol
// EXPECTED_ERROR: (kotlin:7:1) cannot find symbol
