// JDK_KIND: FULL_JDK_21
// MIN_JAVA_VERSION: 16
// EMIT_JVM_TYPE_ANNOTATIONS

package test

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class SourceAnno

@JvmRecord
data class RecordTypeAnnotations(
    val plain: @Anno @SourceAnno String,
    val generic: @Anno @SourceAnno List<@Anno @SourceAnno String>,
)
