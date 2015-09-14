package org.test

@Retention(AnnotationRetention.BINARY)
public annotation class SomeAnnotation

@SomeAnnotation public class SomeClass {

    @SomeAnnotation public fun annotatedFunction() {

    }

}