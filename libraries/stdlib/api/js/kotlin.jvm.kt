@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER})
@kotlin.annotation.Retention(value = AnnotationRetention.SOURCE)
@kotlin.Deprecated(message = "Synchronizing methods on a class instance is not supported on platforms other than JVM. If you need to annotate a common method as JVM-synchronized, introduce your own optional-expectation annotation and actualize it with a typealias to kotlin.jvm.Synchronized.")
@kotlin.DeprecatedSinceKotlin(warningSince = "1.8")
@kotlin.annotation.MustBeDocumented
public final annotation class Synchronized : kotlin.Annotation {
    public constructor Synchronized()
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.FIELD})
@kotlin.annotation.Retention(value = AnnotationRetention.SOURCE)
@kotlin.Deprecated(message = "This annotation has no effect in Kotlin/JS. Use kotlin.concurrent.Volatile annotation in multiplatform code instead.", replaceWith = kotlin.ReplaceWith(expression = "kotlin.concurrent.Volatile", imports = {"kotlin.concurrent.Volatile"}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.9")
@kotlin.annotation.MustBeDocumented
public final annotation class Volatile : kotlin.Annotation {
    public constructor Volatile()
}