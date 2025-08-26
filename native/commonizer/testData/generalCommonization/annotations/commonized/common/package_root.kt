expect annotation class CommonAnnotationForAnnotationClassesOnly(val text: String)

expect annotation class CommonAnnotation(val text: String)

expect var propertyWithoutBackingField: Double
expect val propertyWithBackingField: Double
expect val propertyWithDelegateField: Int
expect val <T : CharSequence> T.propertyWithExtensionReceiver: Int

expect fun function1(text: String): String
expect fun <Q : Number> Q.function2(): Q

expect class AnnotatedClass(value: String) {
    val value: String
}
typealias AnnotatedLiftedUpTypeAlias = AnnotatedClass

expect class AnnotatedNonLiftedUpTypeAlias constructor(value: String) {
    val value: String
}
