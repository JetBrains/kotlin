package kotlin.jvm

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER}) @kotlin.annotation.Retention(value = AnnotationRetention.SOURCE) public final annotation class Synchronized : kotlin.Annotation {
    /*primary*/ public constructor Synchronized()
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.FIELD}) @kotlin.annotation.Retention(value = AnnotationRetention.SOURCE) public final annotation class Volatile : kotlin.Annotation {
    /*primary*/ public constructor Volatile()
}