// EMIT_JVM_TYPE_ANNOTATIONS
// FULL_JDK
// KT-77762 duplicates type annotations emitted by the backend for varargs. Inject distinct annotations
// at the array levels to test Kapt's vararg parameter type-annotation conversion independently.
// A comment before vararg marks the implicit outer array; the one on IntArray marks its nested array.
// INJECT_PARAMETER_TYPE_ANNOTATION: test/Methods|varargs|0|null|Ltest/SourceAnno;|invisible
// INJECT_PARAMETER_TYPE_ANNOTATION: test/Methods|arrayVarargs|0|null|Ltest/SourceAnno;|invisible
// INJECT_PARAMETER_TYPE_ANNOTATION: test/Methods|nestedArrayVarargs|0|null|Ltest/SourceAnno;|invisible
// INJECT_PARAMETER_TYPE_ANNOTATION: test/Methods|primitiveArrayVarargs|0|null|Ltest/Anno;|visible
// INJECT_PARAMETER_TYPE_ANNOTATION: test/Methods|primitiveArrayVarargs|0|[|Ltest/SourceAnno;|invisible
// INJECT_PARAMETER_TYPE_ANNOTATION: test/Methods|nonLastVararg|0|null|Ltest/Anno;|visible

package test

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class SourceAnno

class Methods {
    fun varargs(/*@SourceAnno*/ vararg values: @Anno String) {}
    fun arrayVarargs(/*@SourceAnno*/ vararg values: @Anno Array<@SourceAnno String>) {}
    fun nestedArrayVarargs(/*@SourceAnno*/ vararg values: @Anno Array<@SourceAnno Array<String>>) {}
    fun primitiveArrayVarargs(/*@Anno*/ vararg values: /*@SourceAnno*/ IntArray) {}
    fun nonLastVararg(/*@Anno*/ vararg values: @SourceAnno String, last: @Anno String) {}
}
