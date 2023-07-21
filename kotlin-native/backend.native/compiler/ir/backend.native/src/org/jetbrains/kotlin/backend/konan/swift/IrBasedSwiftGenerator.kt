/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.swift

import org.jetbrains.kotlin.backend.konan.llvm.KonanBinaryInterface
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.*
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
    val swiftImports = mutableListOf<Swift.Import>(Swift.Import.Module("Foundation"))
    val swiftDeclarations = mutableListOf<Swift.Declaration>()
    val cDeclarations = mutableListOf<C>() // FIXME: we shouldn't manually generate c headers for our existing code, but here we are.

    private val initRuntimeIfNeededSpec = swiftDeclarations.add {
        function(
                "initRuntimeIfNeeded",
                attributes = listOf(attribute("_silgen_name", "Kotlin_initRuntimeIfNeeded".literal)),
                visibility = Swift.Declaration.Visibility.PRIVATE,
        ).build()
    }

    fun buildSwiftShimFile(): String = Swift.File(swiftImports, swiftDeclarations).render()

    fun buildSwiftBridgingHeader(): String = C.File(cDeclarations).render()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction) {
        if (!isSupported(declaration)) {
            return
        }

        val name = declaration.name.identifier
        val cName = "__kn_${name}"
        val symbolName = "_" + with(KonanBinaryInterface) { declaration.symbolName }

        cDeclarations.add {
            build(function(
                    returnType = mapTypeToC(declaration.returnType) ?: return,
                    name = cName,
                    arguments = declaration.explicitParameters.map {
                        variable(mapTypeToC(it.type) ?: return, it.name.asString())
                    },
                    attributes = listOf(rawAttribute("asm".variable.call(symbolName.literal)))
            )).build()
        }

        swiftDeclarations.add {
            function(
                    name,
                    parameters = declaration.explicitParameters.map {
                        parameter(it.name.asString(), type = mapTypeToSwift(it.type) ?: return)
                    },
                    type = mapTypeToSwift(declaration.returnType) ?: return,
                    visibility = Swift.Declaration.Visibility.PUBLIC
            ) {
                initRuntimeIfNeededSpec.name.variable.call().build()
                `return`(cName.variable.call(declaration.explicitParameters.map { it.name.asString().variable })).build()
            }.build()
        }
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

    private fun mapTypeToC(declaration: IrType): C.Type? {
        return C.new {
            when {
                declaration.isPrimitiveType() -> if (declaration.isNullable()) null else when(declaration.getPrimitiveType()!!) {
                    PrimitiveType.BYTE -> char.signed
                    PrimitiveType.BOOLEAN -> bool
                    PrimitiveType.CHAR -> null
                    PrimitiveType.SHORT -> short
                    PrimitiveType.INT -> int
                    PrimitiveType.LONG -> long
                    PrimitiveType.FLOAT -> float
                    PrimitiveType.DOUBLE -> double
                }
                declaration.isUnsignedType() -> if (declaration.isNullable()) null else when(declaration.getUnsignedType()!!) {
                    UnsignedType.UBYTE -> char.unsigned
                    UnsignedType.USHORT -> short.unsigned
                    UnsignedType.UINT -> int.unsigned
                    UnsignedType.ULONG -> long.unsigned
                }
                declaration.isUnit() -> if (declaration.isNullable()) null else void
                else -> null
            }
        }
    }

    private fun mapTypeToSwift(declaration: IrType): Swift.Type? {
        return when {
            declaration.isPrimitiveType() -> if (declaration.isNullable()) null else when(declaration.getPrimitiveType()!!) {
                PrimitiveType.BYTE -> "Int8"
                PrimitiveType.BOOLEAN -> "Bool"
                PrimitiveType.CHAR -> null
                PrimitiveType.SHORT -> "Int16"
                PrimitiveType.INT -> "Int32"
                PrimitiveType.LONG -> "Int64"
                PrimitiveType.FLOAT -> "Float"
                PrimitiveType.DOUBLE -> "Double"
            }
            declaration.isUnsignedType() -> if (declaration.isNullable()) null else when(declaration.getUnsignedType()!!) {
                UnsignedType.UBYTE -> "UInt8"
                UnsignedType.USHORT -> "UInt16"
                UnsignedType.UINT -> "UInt32"
                UnsignedType.ULONG -> "UInt64"
            }
            declaration.isUnit() -> if (declaration.isNullable()) null else "Void"
            else -> null
        }?.let { Swift.Type.Nominal("Swift.$it") }
    }
}