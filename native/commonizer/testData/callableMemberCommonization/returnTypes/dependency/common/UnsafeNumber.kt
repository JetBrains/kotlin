package kotlinx.cinterop
@Target(AnnotationTarget.TYPEALIAS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class UnsafeNumber(val actualPlatformTypes: Array<String>)
