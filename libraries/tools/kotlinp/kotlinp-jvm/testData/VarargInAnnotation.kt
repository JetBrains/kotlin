import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE)
annotation class AnnoString(vararg val value: String)

@Target(AnnotationTarget.TYPE)
annotation class AnnoInt(vararg val value: Int)

enum class A { V1, V2 }
@Target(AnnotationTarget.TYPE)
annotation class AnnoEnum(vararg val value: A)

@Target(AnnotationTarget.TYPE)
annotation class AnnoKClass(vararg val value: KClass<*>)

@Target(AnnotationTarget.TYPE)
annotation class AnnoAnnotation(vararg val value: AnnoString)

fun annoStringVararg0(): @AnnoString() Unit {}
fun annoStringVararg1(): @AnnoString("OK") Unit {}
fun annoStringVararg2(): @AnnoString("OK", "OK2") Unit {}

fun annoIntVararg0(): @AnnoInt() Unit {}
fun annoIntVararg1(): @AnnoInt(0) Unit {}
fun annoIntVararg2(): @AnnoInt(0, 1) Unit {}

fun annoEnumVararg0(): @AnnoEnum() Unit {}
fun annoEnumVararg1(): @AnnoEnum(A.V1) Unit {}
fun annoEnumVararg2(): @AnnoEnum(A.V1, A.V2) Unit {}

fun annoKClassVararg0(): @AnnoKClass() Unit {}
fun annoKClassVararg1(): @AnnoKClass(AnnoString::class) Unit {}
fun annoKClassVararg2(): @AnnoKClass(AnnoString::class, AnnoInt::class) Unit {}

fun annoAnnotationVararg0(): @AnnoAnnotation() Unit {}
fun annoAnnotationVararg1(): @AnnoAnnotation(AnnoString()) Unit {}
fun annoAnnotationVararg2(): @AnnoAnnotation(AnnoString("OK"), AnnoString("OK1", "OK2")) Unit {}

fun annoArrayVararg0(): @AnnoString(*arrayOf()) Unit {}
fun annoArrayVararg1(): @AnnoString(*arrayOf("OK")) Unit {}
fun annoArrayVararg2(): @AnnoString(*arrayOf("OK", "OK2")) Unit {}
