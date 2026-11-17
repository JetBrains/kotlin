// EMIT_JVM_TYPE_ANNOTATIONS
// FULL_JDK
// Kotlin's @Throws cannot annotate the exception type itself, so inject annotations to test its stub conversion.
// INJECT_EXCEPTION_TYPE_ANNOTATION: test/Methods|<init>|0|null|Ltest/Anno;|visible
// INJECT_EXCEPTION_TYPE_ANNOTATION: test/Methods|plain|0|null|Ltest/Anno;|visible
// INJECT_EXCEPTION_TYPE_ANNOTATION: test/Methods|plain|1|null|Ltest/SourceAnno;|invisible
// INJECT_EXCEPTION_TYPE_ANNOTATION: test/Methods|generic|0|null|Ltest/SourceAnno;|invisible

package test

import java.io.IOException
import java.sql.SQLException

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class SourceAnno

class Methods @Throws(/*@Anno*/ IOException::class) constructor() {
    @Throws(/*@Anno*/ IOException::class, /*@SourceAnno*/ SQLException::class)
    fun plain() {}

    @Throws(/*@SourceAnno*/ IOException::class)
    fun <T> generic(value: T): T = value
}
