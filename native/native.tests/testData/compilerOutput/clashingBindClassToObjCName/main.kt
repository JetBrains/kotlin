import kotlin.native.internal.reflect.objCNameOrNull

fun main() {
    println(MyClass::class.objCNameOrNull)
}