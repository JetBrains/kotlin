// Same as withoutSource.kt

@Retention(AnnotationRetention.SOURCE)
annotation class SourceAnno

@Retention(AnnotationRetention.BINARY)
annotation class BinaryAnno

@Retention(AnnotationRetention.RUNTIME)
annotation class RuntimeAnno

@SourceAnno
@BinaryAnno
@RuntimeAnno
class Test