class TestClass(@get:SomeAnnotation(annotationArg = "arg1")
                val arg1: String, @get:SomeAnnotation
                val arg2: String)

internal annotation class SomeAnnotation(val annotationArg: String = "")