import OuterTypeA.TargetTypeAA
import OuterTypeB.TargetTypeBA

interface FooAnnotationUser {
    @OuterTypeA.TargetTypeAA fun annotationUserAAOuter()
    @TargetTypeAA fun annotationUserAAInner()
    @OuterTypeB.TargetTypeBA fun annotationUserBAOuter()
    @TargetTypeBA fun annotationUserBAInner()
}
