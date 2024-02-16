// FIR_IDENTICAL
package test

annotation class Empty

annotation class JustAnnotation(val annotation: Empty)

annotation class AnnotationArray(val annotationArray: Array<JustAnnotation>)

@JustAnnotation(Empty())
@AnnotationArray(arrayOf())
class C1

@AnnotationArray(arrayOf(JustAnnotation(Empty()), JustAnnotation(Empty())))
class C2
