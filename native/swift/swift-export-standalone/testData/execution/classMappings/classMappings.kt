// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: ClassMappings
// FILE: classes.kt
import kotlin.native.internal.reflect.objCNameOrNull

class FinalClass {
    class NestedFinalClass
}
open class OpenClass
private class PrivateClass : OpenClass()

fun getAnyClassName(): String? = Any::class.objCNameOrNull
fun getFinalClassName(): String? = FinalClass::class.objCNameOrNull
fun getNestedFinalClassName(): String? = FinalClass.NestedFinalClass::class.objCNameOrNull
fun getOpenClassName(): String? = OpenClass::class.objCNameOrNull
fun getPrivateClassName(): String? = PrivateClass::class.objCNameOrNull

// FILE: classes_in_namespace.kt
package namespace

import kotlin.native.internal.reflect.objCNameOrNull

class NamespacedFinalClass

fun getNamespacedFinalClassName(): String? = NamespacedFinalClass::class.objCNameOrNull