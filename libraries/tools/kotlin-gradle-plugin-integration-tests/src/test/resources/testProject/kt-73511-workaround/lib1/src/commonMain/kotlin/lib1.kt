@RequiresOptIn
annotation class MyAnnotation

@OptIn(ExperimentalSubclassOptIn::class)
@SubclassOptInRequired(MyAnnotation::class)
open class MyAnnotationHolder(val x: Int)
