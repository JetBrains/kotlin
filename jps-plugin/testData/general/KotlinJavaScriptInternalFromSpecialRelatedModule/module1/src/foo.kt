package test1

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
internal annotation class InternalFileAnnotation1()

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
internal annotation class InternalClassAnnotation1()

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
internal annotation class InternalFunctionAnnotation1()

internal open class InternalClass1

abstract class ClassA1(internal val member: Int)

abstract class ClassB1 {
    internal abstract val member: Int
    internal fun func() = 1
}

internal val internalProp = 1

internal fun internalFun() {}

