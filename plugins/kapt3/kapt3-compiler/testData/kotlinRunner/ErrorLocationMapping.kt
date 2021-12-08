// IGNORE_BACKEND: JVM_IR

class Subject {



    val field:String = ""

    @MyAnnotation
    val annotationTrigger: String = ""
}

internal annotation class MyAnnotation
