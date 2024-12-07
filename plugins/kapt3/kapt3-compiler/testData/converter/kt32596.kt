// CORRECT_ERROR_TYPES

@file:Suppress("UNRESOLVED_REFERENCE", "ANNOTATION_ARGUMENT_MUST_BE_CONST")
import kotlin.reflect.KClass

@ABC(x = 42)
class ErrorMissingAnnotation

@ABC @CDE
class ErrorMultipleMissingAnnotations

@CDE @Anno(ABC::class) @ABC
class ErrorSomeMissingAnnotations

annotation class Anno(val klass: KClass<*>)

@com  /* ??? */ . example /* ???? */ . // ???
  /* ??? */ XYZ ("param")
class FullyQualifiedMissingAnnotation
