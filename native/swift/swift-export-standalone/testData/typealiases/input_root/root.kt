public typealias RegularInteger = Int

public typealias DefaultInteger = RegularInteger

public fun increment(integer: DefaultInteger): RegularInteger = integer + 1

@Target(AnnotationTarget.TYPE)
annotation class UselessAnnotation

typealias ShouldHaveNoAnnotation = @UselessAnnotation Int

typealias UselessDeclaration = UselessAnnotation