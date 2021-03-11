package test
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
internal annotation class InternalTestAnnotation()

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
internal annotation class InternalClassAnnotation()

private class PrivateClass1

internal open class InternalClass1

abstract class ClassA1(internal val member: Int)

abstract class ClassB1 {
    internal abstract val member: Int
}

