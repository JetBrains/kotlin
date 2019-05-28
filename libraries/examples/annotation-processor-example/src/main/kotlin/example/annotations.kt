package example

import java.lang.annotation.Inherited

@Inherited
annotation class ExampleAnnotation

@Inherited
@Retention(AnnotationRetention.SOURCE)
annotation class ExampleSourceAnnotation

@Inherited
@Retention(AnnotationRetention.BINARY)
annotation class ExampleBinaryAnnotation

@Inherited
@Retention(AnnotationRetention.RUNTIME)
annotation class ExampleRuntimeAnnotation

annotation class GenError

annotation class KotlinFilerGenerated