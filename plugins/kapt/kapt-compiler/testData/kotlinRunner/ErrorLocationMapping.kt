// EMIT_JVM_TYPE_ANNOTATIONS

class Subject {



    val field:String = ""

    @MyAnnotation
    val annotationTrigger: String = ""

    fun annotatedMethod(value: List<@TypeAnnotation String>) {}
}

internal annotation class MyAnnotation

@Target(AnnotationTarget.TYPE)
annotation class TypeAnnotation
