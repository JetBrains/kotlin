import kotlin.reflect.KClass

private fun nameToString(
    kClass: KClass<*>,
    methodName: String
) = "$kClass - $methodName"

internal fun test(): String {
    return nameToString(A::class, A::testFunction.name)
}
