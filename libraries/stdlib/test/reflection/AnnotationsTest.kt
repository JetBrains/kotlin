@file:kotlin.jvm.JvmVersion
@file:Suppress("DEPRECATION", "DEPRECATED_JAVA_ANNOTATION")
package test.reflection

import kotlin.test.*

@Retention(AnnotationRetention.RUNTIME)
annotation class MyAnno

@MyAnno
@java.lang.Deprecated
class AnnotatedClass


class AnnotationTest {
    @Test fun annotationType() {
        val kAnnotations = AnnotatedClass::class.java.annotations.map { it!!.annotationClass }
        val jAnnotations = AnnotatedClass::class.java.annotations.map { it!!.annotationClass.java }

        assertTrue(kAnnotations.containsAll(listOf(MyAnno::class,       java.lang.Deprecated::class)))
        assertTrue(jAnnotations.containsAll(listOf(MyAnno::class.java,  java.lang.Deprecated::class.java)))
    }
}
