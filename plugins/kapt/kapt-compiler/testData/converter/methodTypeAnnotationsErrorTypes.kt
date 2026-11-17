// EMIT_JVM_TYPE_ANNOTATIONS
// CORRECT_ERROR_TYPES
// FULL_JDK
// LANGUAGE: +ContextParameters

@file:Suppress("UNRESOLVED_REFERENCE")

package test

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class SourceAnno

class Methods(value: @Anno List<Missing>) {
    fun parameter(value: @Anno List<@SourceAnno List<Missing>>) {}
    fun result(): @SourceAnno List<@Anno List<Missing>> = TODO()
    fun @Anno List<Missing>.extension(value: @SourceAnno List<Missing>): @Anno List<Missing> = TODO()

    context(value: @Anno List<Missing>)
    fun contextual(other: @SourceAnno List<Missing>) {}

    var property: @Anno @SourceAnno List<Missing> = TODO()
    var @Anno List<Missing>.extensionProperty: @SourceAnno List<Missing>
        get() = TODO()
        set(value) {}

    suspend fun suspending(value: @Anno List<Missing>): @SourceAnno List<@Anno List<Missing>> = TODO()

    @JvmSuppressWildcards
    suspend fun suppressedSuspending(): @Anno List<@SourceAnno List<Missing>> = TODO()
}
