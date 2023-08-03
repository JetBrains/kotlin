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
    companion object {
        private val initRuntimeIfNeeded = CCode.build {
            function(void, "initRuntimeIfNeeded", attributes = listOf(asm("_Kotlin_initRuntimeIfNeeded")))
        }

        private val switchThreadStateToNative = CCode.build {
            function(void, "switchThreadStateToNative", attributes = listOf(asm("_Kotlin_mm_switchThreadStateNative")))
        }

        private val switchThreadStateToRunnable = CCode.build {
            function(void, "switchThreadStateToRunnable", attributes = listOf(asm("_Kotlin_mm_switchThreadStateRunnable")))
        }
    }

    private val swiftImports = mutableListOf<SwiftCode.Import>(SwiftCode.Import.Module("Foundation"))
    private val swiftDeclarations = mutableListOf<SwiftCode.Declaration>()

    // FIXME: we shouldn't manually generate c headers for our existing code, but here we are.
    private val cDeclarations = CCode.build {
        mutableListOf<CCode>(
                include("stdint.h"),
                declare(initRuntimeIfNeeded),
                declare(switchThreadStateToRunnable),
                declare(switchThreadStateToNative),
        )
    }

    fun buildSwiftShimFile() = SwiftCode.File(swiftImports, swiftDeclarations)

    fun buildSwiftBridgingHeader() = CCode.File(cDeclarations)

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

        cDeclarations.add(CCode.build {
            declare(function(
                    returnType = mapTypeToC(declaration.returnType) ?: return,
                    name = cName,
                    arguments = declaration.explicitParameters.map {
                        variable(mapTypeToC(it.type) ?: return, it.name.asString())
                    },
                    attributes = listOf(asm(symbolName))
            ))
        })

        swiftDeclarations.add(SwiftCode.build {
            function(
                    name,
                    parameters = declaration.explicitParameters.map {
                        parameter(it.name.asString(), type = mapTypeToSwift(it.type) ?: return)
                    },
                    type = mapTypeToSwift(declaration.returnType) ?: return,
                    visibility = SwiftCode.Declaration.Visibility.PUBLIC
            ) {
                +initRuntimeIfNeeded.name!!.identifier.call()
                +switchThreadStateToRunnable.name!!.identifier.call()
                val result = +let("result", value = cName.identifier.call(declaration.explicitParameters.map { it.name.asString().identifier }))
                +switchThreadStateToNative.name!!.identifier.call()
                +`return`(result.name.identifier)
            }
        })
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

    private fun mapTypeToC(declaration: IrType): CCode.Type? {
        return CCode.build {
            when {
                declaration.isPrimitiveType() -> if (declaration.isNullable()) null else when(declaration.getPrimitiveType()!!) {
                    PrimitiveType.BYTE -> int8()
                    PrimitiveType.BOOLEAN -> bool
                    PrimitiveType.CHAR -> null // TODO: implement alongside with strings
                    PrimitiveType.SHORT -> int16()
                    PrimitiveType.INT -> int32()
                    PrimitiveType.LONG -> int64()
                    PrimitiveType.FLOAT -> float
                    PrimitiveType.DOUBLE -> double
                }
                declaration.isUnsignedType() -> if (declaration.isNullable()) null else when(declaration.getUnsignedType()!!) {
                    UnsignedType.UBYTE -> int8(isUnsigned = true)
                    UnsignedType.USHORT -> int16(isUnsigned = true)
                    UnsignedType.UINT -> int32(isUnsigned = true)
                    UnsignedType.ULONG -> int64(isUnsigned = true)
                }
                declaration.isUnit() -> if (declaration.isNullable()) null else void
                else -> null
            }
        }
    }

    private fun mapTypeToSwift(declaration: IrType): SwiftCode.Type? {
        return when {
            declaration.isPrimitiveType() -> if (declaration.isNullable()) null else when(declaration.getPrimitiveType()!!) {
                PrimitiveType.BYTE -> "Int8"
                PrimitiveType.BOOLEAN -> "Bool"
                PrimitiveType.CHAR -> null // TODO: implement alongside with strings
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
        }?.let { SwiftCode.Type.Nominal("Swift.$it") }
    }
}

private fun CCode.Builder.asm(name: String) = rawAttribute("asm".identifier.call(name.literal))
