/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


import org.jetbrains.kotlin.backend.wasm.ast.*
import java.io.File


private fun WasmValueType.toKotlin(): String = when (this) {
    WasmI32 -> "Int"
    WasmI64 -> "Long"
    WasmF32 -> "Float"
    WasmF64 -> "Double"
    WasmAnyRef -> "Any?"
    WasmNullRefType -> "Nothing?"
    is WasmStructRef, WasmUnreachableType -> error("Unsupported")
    WasmI1 -> "Boolean"
}

private inline fun <reified T> generateIntrinsicAnnotation(annotationName: String): String
        where T : Enum<T>,
              T : WasmTypedOp {

    val annotation = """
@file:ExcludedFromCodegen
@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION", "NON_ABSTRACT_FUNCTION_WITH_NO_BODY", "unused")

package kotlin.wasm.internal

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@ExcludedFromCodegen
internal annotation class $annotationName(val name: String) {
    companion object {
${enumValues<T>().joinToString("\n") { "    const val ${it.name} = \"${it.name}\"" }}
    }
}
"""
    val intrinsics = enumValues<T>().joinToString("\n") { op ->
        val operands = (0 until op.operandsCount).joinToString(", ") { "${'a' + it}: ${op.getOperandType(it).toKotlin()}" }
        """
        @$annotationName($annotationName.${op.name})
        external fun wasm_${op.name.toLowerCase()}($operands): ${op.type?.toKotlin() ?: "Unit"}
        
        """.trimIndent()
    }

    return annotation + "\n" + intrinsics
}

fun main(args: Array<String>) {
    val rootDir = File(args.single())
    val intrinsicsDir = rootDir.resolve("libraries/stdlib/wasm/internal/intrinsics")

    intrinsicsDir.resolve("WasmUnaryOp.kt")
        .writeText(generateIntrinsicAnnotation<WasmUnaryOp>("WasmUnaryOp"))

    intrinsicsDir.resolve("WasmBinaryOp.kt")
        .writeText(generateIntrinsicAnnotation<WasmBinaryOp>("WasmBinaryOp"))

    intrinsicsDir.resolve("WasmLoadOp.kt")
        .writeText(generateIntrinsicAnnotation<WasmLoadOp>("WasmLoadOp"))

    intrinsicsDir.resolve("WasmStoreOp.kt")
        .writeText(generateIntrinsicAnnotation<WasmStoreOp>("WasmStoreOp"))

    intrinsicsDir.resolve("WasmRefOp.kt")
        .writeText(generateIntrinsicAnnotation<WasmRefOp>("WasmRefOp"))
}