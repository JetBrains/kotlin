// CORRECT_ERROR_TYPES
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// ^^^ `@file:Suppress("UNRESOLVED_REFERENCE")` causes the visibility checker to crash when checking annotation visibility

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
