// EMIT_JVM_TYPE_ANNOTATIONS
// FULL_JDK
// KT-77762 duplicates backend-emitted vararg annotations onto the implicit outer array.

package test

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class SourceAnno

class Simple(vararg val values: @Anno @SourceAnno String)

class Generic(vararg val values: @Anno List<@SourceAnno Number>)

class ArrayElements(vararg val values: @Anno Array<@SourceAnno String>)

class NestedArrayElements(vararg val values: @Anno Array<@SourceAnno Array<@Anno String>>)

class PrimitiveElements(vararg val values: @Anno @SourceAnno Int)

class PrimitiveArrayElements(vararg val values: @Anno IntArray)

class TypeVariableElements<T>(vararg val values: @Anno @SourceAnno T)

class NonLastVararg(
    val first: @SourceAnno String,
    vararg val values: @Anno List<@SourceAnno Number>,
    val last: @Anno String,
)
