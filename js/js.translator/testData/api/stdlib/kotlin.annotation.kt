package kotlin.annotation

public final enum class AnnotationRetention : kotlin.Enum<kotlin.annotation.AnnotationRetention> {
    enum entry SOURCE

    enum entry BINARY

    enum entry RUNTIME

    // Static members
    public final /*synthesized*/ fun valueOf(/*0*/ value: kotlin.String): kotlin.annotation.AnnotationRetention
    public final /*synthesized*/ fun values(): kotlin.Array<kotlin.annotation.AnnotationRetention>
}

public final enum class AnnotationRetention : kotlin.Enum<kotlin.annotation.AnnotationRetention> {
    enum entry SOURCE

    enum entry BINARY

    enum entry RUNTIME

    // Static members
    public final /*synthesized*/ fun valueOf(/*0*/ value: kotlin.String): kotlin.annotation.AnnotationRetention
    public final /*synthesized*/ fun values(): kotlin.Array<kotlin.annotation.AnnotationRetention>
}

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

    @kotlin.SinceKotlin(version = "1.1") enum entry TYPEALIAS

    // Static members
    public final /*synthesized*/ fun valueOf(/*0*/ value: kotlin.String): kotlin.annotation.AnnotationTarget
    public final /*synthesized*/ fun values(): kotlin.Array<kotlin.annotation.AnnotationTarget>
}

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

    @kotlin.SinceKotlin(version = "1.1") enum entry TYPEALIAS

    // Static members
    public final /*synthesized*/ fun valueOf(/*0*/ value: kotlin.String): kotlin.annotation.AnnotationTarget
    public final /*synthesized*/ fun values(): kotlin.Array<kotlin.annotation.AnnotationTarget>
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS}) public final annotation class MustBeDocumented : kotlin.Annotation {
    /*primary*/ public constructor MustBeDocumented()
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS}) public final annotation class MustBeDocumented : kotlin.Annotation {
    /*primary*/ public constructor MustBeDocumented()
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS}) public final annotation class Repeatable : kotlin.Annotation {
    /*primary*/ public constructor Repeatable()
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS}) public final annotation class Repeatable : kotlin.Annotation {
    /*primary*/ public constructor Repeatable()
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS}) public final annotation class Retention : kotlin.Annotation {
    /*primary*/ public constructor Retention(/*0*/ value: kotlin.annotation.AnnotationRetention = ...)
    public final val value: kotlin.annotation.AnnotationRetention
        public final fun <get-value>(): kotlin.annotation.AnnotationRetention
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS}) public final annotation class Retention : kotlin.Annotation {
    /*primary*/ public constructor Retention(/*0*/ value: kotlin.annotation.AnnotationRetention = ...)
    public final val value: kotlin.annotation.AnnotationRetention
        public final fun <get-value>(): kotlin.annotation.AnnotationRetention
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS}) @kotlin.annotation.MustBeDocumented public final annotation class Target : kotlin.Annotation {
    /*primary*/ public constructor Target(/*0*/ vararg allowedTargets: kotlin.annotation.AnnotationTarget /*kotlin.Array<out kotlin.annotation.AnnotationTarget>*/)
    public final val allowedTargets: kotlin.Array<out kotlin.annotation.AnnotationTarget>
        public final fun <get-allowedTargets>(): kotlin.Array<out kotlin.annotation.AnnotationTarget>
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS}) @kotlin.annotation.MustBeDocumented public final annotation class Target : kotlin.Annotation {
    /*primary*/ public constructor Target(/*0*/ vararg allowedTargets: kotlin.annotation.AnnotationTarget /*kotlin.Array<out kotlin.annotation.AnnotationTarget>*/)
    public final val allowedTargets: kotlin.Array<out kotlin.annotation.AnnotationTarget>
        public final fun <get-allowedTargets>(): kotlin.Array<out kotlin.annotation.AnnotationTarget>
}