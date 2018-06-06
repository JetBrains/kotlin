annotation class Annotation

@Annotation
class A

annotation class AnnotationInner(val value: Annotation)

@AnnotationArray(Annotation())
class B1

@AnnotationArray(value = Annotation())
class B2

annotation class AnnotationArray(vararg val value: Annotation)

@AnnotationArray(Annotation())
class C