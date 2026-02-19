// WITH_REFLECT

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann1(val v: String)

class A

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann2(val v: KClass<*>)

enum class E {
    ONE
}

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann3(val v: E)

@Ann1("K")
@Ann2(A::class)
@Ann3(E.ONE)
fun foo() = "O"

val av = ::foo.annotations.mapNotNull {
    when (it) {
        is Ann1 -> it.v
        is Ann2 -> it.v.simpleName
        is Ann3 -> it.v.toString()
        else -> null
    }
}.joinToString("_")

val rv = foo() + av

// expected: rv: OK_A_ONE