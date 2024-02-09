// FIR_IDENTICAL
package test
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE)
annotation class AnnoRuntime
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.TYPE)
annotation class AnnoBinary
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE)
annotation class AnnoSource

fun withRuntimeAnnotation(id: @AnnoRuntime Int) {}
fun withBinaryAnnotation(id: @AnnoBinary Int) {}
fun withSourceAnnotation(id: @AnnoSource Int) {}
