// PROBLEM: none

@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.PROPERTY,
)
annotation class Range(val min: Long = 0)

@Range(min = <caret>-1)
val x: Int = 1