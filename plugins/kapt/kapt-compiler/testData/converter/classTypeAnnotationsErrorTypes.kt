// EMIT_JVM_TYPE_ANNOTATIONS
// CORRECT_ERROR_TYPES
// FULL_JDK

@file:Suppress("UNRESOLVED_REFERENCE")

package test

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class SourceAnno

open class Base
interface First
interface Second

// Reconstructing missing supertypes must not discard or shift annotations on resolved supertypes.
class MissingBase : Missing(), @Anno First, @SourceAnno Second
class MissingInterface : @Anno Base(), Missing, @SourceAnno First
