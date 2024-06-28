import kotlin.experimental.ExperimentalObjCRefinement

@ExperimentalObjCRefinement
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@kotlin.native.HidesFromObjC
annotation class MyHiddenFromObjC

@OptIn(ExperimentalObjCRefinement::class)
@MyHiddenFromObjC
class Hidden

class NotHidden