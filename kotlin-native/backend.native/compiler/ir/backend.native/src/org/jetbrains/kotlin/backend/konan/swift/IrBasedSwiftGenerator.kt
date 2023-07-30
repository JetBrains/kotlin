/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.swift

import io.outfoxx.swiftpoet.*
import org.jetbrains.kotlin.backend.konan.llvm.KonanBinaryInterface
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.allParameters
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * Generate a Swift API file for the given Kotlin IR module.
 *
 * A temporary solution to kick-start the work on Swift Export.
 * A proper solution is likely to be FIR-based and will be added later
 * as it requires a bit more work.
 *
 */
class IrBasedSwiftGenerator(private val moduleName: String) : IrElementVisitorVoid {

    private val initRuntimeIfNeededSpec = FunctionSpec.abstractBuilder("initRuntimeIfNeeded")
            // FIXME: _silgen_name only work for as long as swiftcc matches ccc. Switch to c brindging instead.
            .addAttribute("_silgen_name", "\"Kotlin_initRuntimeIfNeeded\"")
            .addModifiers(Modifier.PRIVATE)
            .build()

    private val switchThreadStateNative = FunctionSpec.abstractBuilder("switchThreadStateNative")
            // FIXME: _silgen_name only work for as long as swiftcc matches ccc. Switch to c brindging instead.
            .addAttribute("_silgen_name", "\"Kotlin_mm_switchThreadStateNative\"")
            .addModifiers(Modifier.PRIVATE)
            .build()

    private val switchThreadStateRunnable = FunctionSpec.abstractBuilder("switchThreadStateRunnable")
            // FIXME: _silgen_name only work for as long as swiftcc matches ccc. Switch to c brindging instead.
            .addAttribute("_silgen_name", "\"Kotlin_mm_switchThreadStateRunnable\"")
            .addModifiers(Modifier.PRIVATE)
            .build()

    val functions = mutableListOf<FunctionSpec>(initRuntimeIfNeededSpec, switchThreadStateNative, switchThreadStateRunnable)

    fun build(): FileSpec =
        FileSpec.builder(moduleName, moduleName).apply {
            functions.forEach { topLevelFunction ->
                addFunction(topLevelFunction)
            }
        }.build()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction) {
        if (!isSupported(declaration)) {
            return
        }

        val name = declaration.name.identifier
        val symbolName = with(KonanBinaryInterface) { declaration.symbolName }

        val returnTypeSpec = mapType(declaration.returnType) ?: return
        val parametersSpec = declaration.explicitParameters.map {
            ParameterSpec.builder(it.name.asString(), mapType(it.type) ?: return).build()
        }

        val forwardDeclarationSpec = FunctionSpec.abstractBuilder("${name}_bridge")
                // FIXME: _silgen_name only work for as long as swiftcc matches ccc. Switch to c brindging instead.
                .addAttribute("_silgen_name", "\"${symbolName}\"")
                .addModifiers(Modifier.PRIVATE)
                .returns(returnTypeSpec)
                .apply {
                    for (parameter in parametersSpec) {
                        addParameter(parameter)
                    }
                }
                .build()

        val shimFunctionSpec = FunctionSpec.builder(name)
                .addModifiers(Modifier.PUBLIC)
                .returns(returnTypeSpec)
                .apply {
                    for (parameter in parametersSpec) {
                        addParameter(parameter)
                    }
                }
                .addStatement("${initRuntimeIfNeededSpec.name}()")
                .addStatement("${switchThreadStateRunnable.name}()")
                .addStatement("let result = ${forwardDeclarationSpec.name}(${parametersSpec.map { "${it.parameterName}: ${it.parameterName}" }.joinToString()})")
                .addStatement("${switchThreadStateNative.name}()")
                .addStatement("return result")
                .build()

        functions += forwardDeclarationSpec
        functions += shimFunctionSpec
    }

    private fun isSupported(declaration: IrFunction): Boolean {
        // No Kotlin-exclusive stuff
        return declaration.visibility.isPublicAPI
                && declaration.extensionReceiverParameter == null
                && declaration.dispatchReceiverParameter == null
                && declaration.contextReceiverParametersCount == 0
                && !declaration.isExpect
                && !declaration.isInline
    }

    private fun mapType(declaration: IrType): TypeName? {
        val swiftPrimitiveTypeName: String? = declaration.getPrimitiveType().takeUnless { declaration.isNullable() }?.let {
            when (it) {
                PrimitiveType.BYTE -> "Int8"
                PrimitiveType.BOOLEAN -> "Bool"
                PrimitiveType.CHAR -> null
                PrimitiveType.SHORT -> "Int16"
                PrimitiveType.INT -> "Int32"
                PrimitiveType.LONG -> "Int64"
                PrimitiveType.FLOAT -> "Float"
                PrimitiveType.DOUBLE -> "Double"
            }
        } ?: declaration.getUnsignedType().takeUnless { declaration.isNullable() }?.let {
            when (it) {
                UnsignedType.UBYTE -> "UInt8"
                UnsignedType.USHORT -> "UInt16"
                UnsignedType.UINT -> "UInt32"
                UnsignedType.ULONG -> "UInt64"
            }
        } ?: if (declaration.isUnit()) "Void" else null

        if (swiftPrimitiveTypeName == null) {
            println("Failed to bridge ${declaration.classFqName}")
        }

        return swiftPrimitiveTypeName?.let { DeclaredTypeName.typeName("Swift.$it") }
    }
}