public final enum class AnnotationRetention : kotlin.Enum<kotlin.annotation.AnnotationRetention> {
    enum entry SOURCE

    enum entry BINARY

    enum entry RUNTIME
}

/*∆*/ public final enum class AnnotationRetention : kotlin.Enum<kotlin.annotation.AnnotationRetention> {
/*∆*/     enum entry SOURCE
/*∆*/ 
/*∆*/     enum entry BINARY
/*∆*/ 
/*∆*/     enum entry RUNTIME
/*∆*/ }
/*∆*/ 
public final enum class AnnotationTarget : kotlin.Enum<kotlin.annotation.AnnotationTarget> {
    enum entry CLASS

    enum entry ANNOTATION_CLASS

    enum entry TYPE_PARAMETER

    enum entry PROPERTY

    enum entry FIELD

    enum entry LOCAL_VARIABLE

    enum entry VALUE_PARAMETER

    enum entry CONSTRUCTOR

    enum entry FUNCTION

    enum entry PROPERTY_GETTER

    enum entry PROPERTY_SETTER

    enum entry TYPE

    enum entry EXPRESSION

    enum entry FILE

    @kotlin.SinceKotlin(version = "1.1")
    enum entry TYPEALIAS
}

/*∆*/ public final enum class AnnotationTarget : kotlin.Enum<kotlin.annotation.AnnotationTarget> {
/*∆*/     enum entry CLASS
/*∆*/ 
/*∆*/     enum entry ANNOTATION_CLASS
/*∆*/ 
/*∆*/     enum entry TYPE_PARAMETER
/*∆*/ 
/*∆*/     enum entry PROPERTY
/*∆*/ 
/*∆*/     enum entry FIELD
/*∆*/ 
/*∆*/     enum entry LOCAL_VARIABLE
/*∆*/ 
/*∆*/     enum entry VALUE_PARAMETER
/*∆*/ 
/*∆*/     enum entry CONSTRUCTOR
/*∆*/ 
/*∆*/     enum entry FUNCTION
/*∆*/ 
/*∆*/     enum entry PROPERTY_GETTER
/*∆*/ 
/*∆*/     enum entry PROPERTY_SETTER
/*∆*/ 
/*∆*/     enum entry TYPE
/*∆*/ 
/*∆*/     enum entry EXPRESSION
/*∆*/ 
/*∆*/     enum entry FILE
/*∆*/ 
/*∆*/     @kotlin.SinceKotlin(version = "1.1")
/*∆*/     enum entry TYPEALIAS
/*∆*/ }
/*∆*/ 
@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS})
public final annotation class MustBeDocumented : kotlin.Annotation {
    public constructor MustBeDocumented()
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS})
/*∆*/ public final annotation class MustBeDocumented : kotlin.Annotation {
/*∆*/     public constructor MustBeDocumented()
/*∆*/ }
/*∆*/ 
/*∆*/ @kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS})
public final annotation class Repeatable : kotlin.Annotation {
    public constructor Repeatable()
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS})
/*∆*/ public final annotation class Repeatable : kotlin.Annotation {
/*∆*/     public constructor Repeatable()
/*∆*/ }
/*∆*/ 
/*∆*/ @kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS})
public final annotation class Retention : kotlin.Annotation {
    public constructor Retention(value: kotlin.annotation.AnnotationRetention = ...)

    public final val value: kotlin.annotation.AnnotationRetention { get; }
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS})
/*∆*/ public final annotation class Retention : kotlin.Annotation {
/*∆*/     public constructor Retention(value: kotlin.annotation.AnnotationRetention = ...)
/*∆*/ 
/*∆*/     public final val value: kotlin.annotation.AnnotationRetention { get; }
/*∆*/ }
/*∆*/ 
/*∆*/ @kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS})
@kotlin.annotation.MustBeDocumented
public final annotation class Target : kotlin.Annotation {
    public constructor Target(vararg allowedTargets: kotlin.annotation.AnnotationTarget)

    public final val allowedTargets: kotlin.Array<out kotlin.annotation.AnnotationTarget> { get; }
}
/*∆*/ 
/*∆*/ @kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS})
/*∆*/ @kotlin.annotation.MustBeDocumented
/*∆*/ public final annotation class Target : kotlin.Annotation {
/*∆*/     public constructor Target(vararg allowedTargets: kotlin.annotation.AnnotationTarget)
/*∆*/ 
/*∆*/     public final val allowedTargets: kotlin.Array<out kotlin.annotation.AnnotationTarget> { get; }
/*∆*/ }