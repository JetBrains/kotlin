// RUN_PIPELINE_TILL: BACKEND

package foo

import org.jetbrains.kotlin.plugin.sandbox.SupertypeWithTypeArgument

interface InterfaceWithArgument<T> {
    fun generate(): T = null!!
}

@SupertypeWithTypeArgument(String::class)
enum class GeneratedEnum

fun test(value: GeneratedEnum): String = value.generate()

/* GENERATED_FIR_TAGS: checkNotNullCall, classReference, enumDeclaration, functionDeclaration, interfaceDeclaration,
nullableType, typeParameter */
