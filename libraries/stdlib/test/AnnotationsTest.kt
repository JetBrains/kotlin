package test.annotations

import kotlin.*
import kotlin.test.assertTrue
import org.junit.Test as test

@Retention(AnnotationRetention.RUNTIME)
annotation class MyAnno

@MyAnno
@java.lang.Deprecated
class AnnotatedClass


class AnnotationTest {
    @test fun annotationType() {
        val annotations = javaClass<AnnotatedClass>().getAnnotations()!!
        
        var foundMyAnno = false
        var foundDeprecated = false
        
        for (annotation in annotations) {
            val clazz = annotation!!.annotationType()
            when {
                clazz == javaClass<MyAnno>() -> foundMyAnno = true
                clazz == javaClass<java.lang.Deprecated>() -> foundDeprecated = true
                else -> {}
            }
        }
        
        assertTrue(foundMyAnno)
        assertTrue(foundDeprecated)
    }
}
