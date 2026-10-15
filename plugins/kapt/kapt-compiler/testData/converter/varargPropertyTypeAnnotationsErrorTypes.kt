// EMIT_JVM_TYPE_ANNOTATIONS
// CORRECT_ERROR_TYPES
// FULL_JDK
// KT-77762 duplicates backend-emitted vararg annotations onto the implicit outer array.

@file:Suppress("UNRESOLVED_REFERENCE")

package test

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class SourceAnno

// Annotations on the missing element are lost; KT-77762 still emits their duplicates on the implicit array.
class Simple(vararg val values: @Anno @SourceAnno Missing)

class Generic(vararg val values: @Anno List<@SourceAnno List<Missing>>)

class ArrayElements(vararg val values: @Anno Array<@SourceAnno List<Missing>>)

class MissingArrayElements(vararg val values: @Anno Array<@SourceAnno Missing>)

class NestedArrayElements(vararg val values: @Anno Array<@SourceAnno Array<@Anno List<Missing>>>)

class NonLastVararg(
    val first: @SourceAnno String,
    vararg val values: @Anno List<@SourceAnno List<Missing>>,
    val last: @Anno String,
)

// Exercise the signature/ASM fallback with error correction enabled.
class ResolvedGeneric(vararg val values: @Anno List<@SourceAnno Number>)
