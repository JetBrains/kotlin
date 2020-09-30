// PROBLEM: none

@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CLASS,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FILE,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.EXPRESSION,
)
annotation class Range(val min: Long = 0)

@Range(min = -1)
val x: @Range(min = <caret>-1) Int = 1