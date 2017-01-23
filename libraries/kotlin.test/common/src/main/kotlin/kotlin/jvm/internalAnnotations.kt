package kotlin.jvm

@Suppress("HEADER_WITHOUT_IMPLEMENTATION")
@Target(AnnotationTarget.FILE)
internal header annotation class JvmMultifileClass

@Suppress("HEADER_WITHOUT_IMPLEMENTATION")
@Target(AnnotationTarget.FILE)
internal header annotation class JvmName(val name: String)
