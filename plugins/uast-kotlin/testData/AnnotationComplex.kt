annotation class Annotation(vararg val strings: String)

@Annotation
class A

annotation class AnnotationInner(val value: Annotation)

@AnnotationArray(Annotation())
class B1

@AnnotationArray(value = Annotation("sv1", "sv2"))
class B2

annotation class AnnotationArray(vararg val value: Annotation)

@AnnotationArray(Annotation(strings = arrayOf("sar1", "sar2")))
class C

@AnnotationArray(Annotation(strings = ["[sar]1", "[sar]2"]))
class C2
