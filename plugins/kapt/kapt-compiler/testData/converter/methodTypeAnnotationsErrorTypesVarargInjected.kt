// EMIT_JVM_TYPE_ANNOTATIONS
// CORRECT_ERROR_TYPES
// FULL_JDK
// KT-77762 duplicates type annotations emitted by the backend for varargs. Inject distinct annotations
// at the array levels to test Kapt's vararg parameter type-annotation conversion independently.
// A comment before vararg marks the implicit outer array, which is not written in the type.
// INJECT_PARAMETER_TYPE_ANNOTATION: test/Methods|varargs|0|null|Ltest/SourceAnno;|invisible
// INJECT_PARAMETER_TYPE_ANNOTATION: test/Methods|nonLastVararg|0|null|Ltest/SourceAnno;|invisible
// INJECT_PARAMETER_TYPE_ANNOTATION: test/Methods|arrayVarargs|0|null|Ltest/SourceAnno;|invisible
// INJECT_PARAMETER_TYPE_ANNOTATION: test/Methods|nestedArrayVarargs|0|null|Ltest/SourceAnno;|invisible

@file:Suppress("UNRESOLVED_REFERENCE")

package test

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class SourceAnno

class Methods {
    fun varargs(/*@SourceAnno*/ vararg values: @Anno List<Missing>) {}
    fun nonLastVararg(/*@SourceAnno*/ vararg values: @Anno List<Missing>, last: @SourceAnno String) {}
    fun arrayVarargs(/*@SourceAnno*/ vararg values: @Anno Array<@SourceAnno List<Missing>>) {}
    fun nestedArrayVarargs(/*@SourceAnno*/ vararg values: @Anno Array<@SourceAnno Array<List<Missing>>>) {}
    fun nonLastNestedArrayVararg(vararg values: @Anno Array<@SourceAnno Array<List<Missing>>>, last: String) {}
}
