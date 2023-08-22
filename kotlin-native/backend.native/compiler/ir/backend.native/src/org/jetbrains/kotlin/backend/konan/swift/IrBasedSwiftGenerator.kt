/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.swift

import org.jetbrains.kotlin.backend.konan.llvm.KonanBinaryInterface
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.cast

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

        // id Kotlin_SwiftExport_refToSwiftObject(ObjHeader *obj);
        private val refToSwiftObject = CCode.build {
            function(void.pointer, "refToSwiftObject", arguments = listOf(variable(void.pointer)), attributes = listOf(asm("_Kotlin_SwiftExport_refToSwiftObject")))
        }

        // ObjHeader *Kotlin_SwiftExport_swiftObjectToRef(id obj);
        private val swiftObjectToRef = CCode.build {
            function(void.pointer, "swiftObjectToRef", arguments = listOf(variable(void.pointer), variable(void.pointer)), attributes = listOf(asm("_Kotlin_SwiftExport_swiftObjectToRef")))
        }

        private val enterFrame = CCode.build {
            function(void, "EnterFrame", listOf(variable(void.pointer), variable(int), variable(int)))
        }

        private val leaveFrame = CCode.build {
            function(void, "LeaveFrame", listOf(variable(void.pointer), variable(int), variable(int)))
        }

        private val bridgeFromKotlin = SwiftCode.build {
            val T = "T".type
            function(
                    "bridgeFromKotlin",
                    parameters = listOf(parameter(parameterName = "obj", type = "UnsafeMutableRawPointer".type)),
                    genericTypes = listOf(T.name.genericParameter(constraint = "AnyObject".type)),
                    returnType = T,
                    attributes = listOf(attribute("inline", "__always".identifier)),
                    visibility = private
            ) {
                +let("ptr", value = refToSwiftObject.name!!.identifier.call("obj".identifier))
                +`return`("Unmanaged".type.withGenericArguments(T).access("fromOpaque").call("ptr".identifier).access("takeUnretainedValue").call())
            }
        }

        private val bridgeToKotlin = SwiftCode.build {
            val T = "T".type
            function(
                    "bridgeToKotlin",
                    parameters = listOf(parameter(parameterName = "obj", type = T), parameter(argumentName = "slot", type = "UnsafeMutableRawPointer".type)),
                    genericTypes = listOf(T.name.genericParameter(constraint = "AnyObject".type)),
                    returnType = "UnsafeMutableRawPointer".type,
                    attributes = listOf(attribute("inline", "__always".identifier)),
                    visibility = private
            ) {
                +let("ptr", value = "Unmanaged".type.withGenericArguments(T).access("passUnretained").call("obj".identifier).access("toOpaque").call())
                +`return`(swiftObjectToRef.name!!.identifier.call("ptr".identifier, "slot".identifier))
            }
        }

        private val pointerExtensions = SwiftCode.build {
            """
            extension UnsafeMutableBufferPointer {
                subscript(pointerAt offset: Int) -> UnsafeMutablePointer<Element> {
                    return self.baseAddress!.advanced(by: offset)
                }
            }
            """.trimIndent().declaration()
        }

        private val withUnsafeTemporaryBufferAllocation = SwiftCode.build {
            """
            func withUnsafeTemporaryBufferAllocation<H, E, R>(
                ofHeader: H.Type = H.self,
                element: E.Type = E.self,
                count: Int,
                body: (UnsafeMutablePointer<H>, UnsafeMutableBufferPointer<E>) throws -> R
            ) rethrows -> R {
                assert(count >= 0)
                assert(MemoryLayout<E>.size > 0)
                
                let headerElementsCount = MemoryLayout<H>.size == 0 ? 0 : 1 + (MemoryLayout<H>.stride - 1) / MemoryLayout<E>.stride
                
                return try withUnsafeTemporaryAllocation(of: E.self, capacity: count + headerElementsCount) { buffer in
                    try buffer.baseAddress!.withMemoryRebound(to: H.self, capacity: 1) { header in
                        try body(header, .init(rebasing: buffer[headerElementsCount...]))
                    }
                }
            }
            """.trimIndent().declaration(attributes = listOf(attribute("inline", "__always".identifier)))
        }

        private val withUnsafeSlots = SwiftCode.build {
            """
            func withUnsafeSlots<R>(
                count: Int,
                body: (UnsafeMutableBufferPointer<UnsafeMutableRawPointer>) throws -> R
            ) rethrows -> R {
                guard count > 0 else { return try body(.init(start: nil, count: 0)) }
                return try withUnsafeTemporaryBufferAllocation(ofHeader: KObjHolderFrameInfo.self, element: UnsafeMutableRawPointer.self, count: count) { header, slots in
                    header.initialize(to: .init(count: UInt32(count)))
                    EnterFrame(header, 0, CInt(count))
                    defer { LeaveFrame(header, 0, CInt(count))}
                    return try body(slots)
                }
            }
            """.trimIndent().declaration(attributes = listOf(attribute("inline", "__always".identifier)))
        }

        private val objHolder = SwiftCode.build {
            """
            private struct KObjHolderFrameInfo {
                var arena: UnsafeMutableRawPointer? = nil
                var previous: UnsafeMutableRawPointer? = nil
                var parameters: UInt32 = 0
                var count: UInt32
            }
            """.trimIndent().declaration()
        }
    }

    private interface BridgeCodeGenDelegate {
        fun getNextSlot(): SwiftCode.Expression
    }

    private sealed interface Bridge {
        val swiftType: SwiftCode.Type
        val cType: CCode.Type
        fun from(expr: SwiftCode.Expression, delegate: BridgeCodeGenDelegate): SwiftCode.Expression
        fun into(expr: SwiftCode.Expression, delegate: BridgeCodeGenDelegate): SwiftCode.Expression

        data class Object(override val swiftType: SwiftCode.Type, override val cType: CCode.Type): Bridge {
            override fun from(expr: SwiftCode.Expression, delegate: BridgeCodeGenDelegate) = SwiftCode.build {
                bridgeFromKotlin.name.identifier.call(expr)
            }

            override fun into(expr: SwiftCode.Expression, delegate: BridgeCodeGenDelegate) = SwiftCode.build {
                bridgeToKotlin.name.identifier.call(expr, "slot" of delegate.getNextSlot())
            }
        }

        data class AsIs(override val swiftType: SwiftCode.Type, override val cType: CCode.Type): Bridge {
            override fun from(expr: SwiftCode.Expression, delegate: BridgeCodeGenDelegate) = expr
            override fun into(expr: SwiftCode.Expression, delegate: BridgeCodeGenDelegate) = expr
        }
    }

    private val swiftImports = mutableListOf<SwiftCode.Import>(SwiftCode.Import.Module("Foundation"))
    private val swiftDeclarations = mutableListOf<SwiftCode.Declaration>(
            bridgeFromKotlin,
            bridgeToKotlin,
            withUnsafeSlots,
            withUnsafeTemporaryBufferAllocation,
            objHolder,
            pointerExtensions,
    )

    // FIXME: we shouldn't manually generate c headers for our existing code, but here we are.
    private val cImports = CCode.build {
        mutableListOf<CCode>(
                include("stdint.h"),
        )
    }
    private val cDeclarations = CCode.build {
        mutableListOf<CCode>(
                declare(initRuntimeIfNeeded),
                declare(switchThreadStateToRunnable),
                declare(switchThreadStateToNative),
                declare(refToSwiftObject),
                declare(swiftObjectToRef),
                declare(enterFrame),
                declare(leaveFrame),
        )
    }

    fun buildSwiftShimFile() = SwiftCode.File(swiftImports, swiftDeclarations)

    fun buildSwiftBridgingHeader() = CCode.build {
        CCode.File(cImports + pragma("clang assume_nonnull begin") + cDeclarations + pragma("clang assume_nonnull end"))
    }

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
        val hasObjHolderParameter = declaration.returnType.isClass

        cDeclarations.add(CCode.build {
            declare(function(
                    returnType = mapTypeToC(declaration.returnType) ?: return,
                    name = cName,
                    arguments = declaration.explicitParameters.map {
                        variable(mapTypeToC(it.type) ?: return, it.name.asString())
                    } + listOfNotNull(variable(void.pointer, "returnSlot").takeIf { hasObjHolderParameter }),
                    attributes = listOf(asm(symbolName))
            ))
        })

        swiftDeclarations.add(SwiftCode.build {
            val returnTypeBridge = bridgeFor(declaration.returnType) ?: return
            val parameterBridges = declaration.explicitParameters.map { it.name.asString() to (bridgeFor(it.type) ?: return) }

            function(
                    name,
                    parameters = parameterBridges.map { parameter(it.first, type = it.second.swiftType) },
                    returnType = returnTypeBridge.swiftType,
                    visibility = public
            ) {
                +initRuntimeIfNeeded.name!!.identifier.call()
                +switchThreadStateToRunnable.name!!.identifier.call()

                val call = let {
                    val bridgeDelegate = object : BridgeCodeGenDelegate {
                        var slotsCount: Int = 0
                        override fun getNextSlot() = "slots".identifier.subscript("pointerAt" of slotsCount++.literal)
                    }

                    val parameters = listOf(
                            parameterBridges.map { it.second.into(it.first.identifier, bridgeDelegate) },
                            listOfNotNull(if (hasObjHolderParameter) bridgeDelegate.getNextSlot() else null)
                    ).flatten()

                    if (bridgeDelegate.slotsCount > 0) {
                        "withUnsafeSlots".identifier.call(
                                "count" of bridgeDelegate.slotsCount.literal,
                                "body" of closure(parameters = listOf(closureParameter("slots"))) {
                                    +returnTypeBridge.from(cName.identifier.call(parameters), bridgeDelegate)
                                }
                        )
                    } else {
                        returnTypeBridge.from(cName.identifier.call(parameters), bridgeDelegate)
                    }
                }

                val result = +let(
                        "result",
                        type = returnTypeBridge.swiftType,
                        value = call
                )
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

    private fun mapTypeToC(declaration: IrType): CCode.Type? = CCode.build {
        when {
            declaration.isUnit() -> if (declaration.isNullable()) null else void
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
            declaration.isRegularClass -> void.pointer(nullability = CCode.Type.Pointer.Nullability.NULLABLE.takeIf { declaration.isNullable() })
            else -> null
        }
    }

    private fun mapTypeToSwift(declaration: IrType): SwiftCode.Type? = SwiftCode.build {
        when {
            declaration.isUnit() -> if (declaration.isNullable()) null else "Void".type
            declaration.isPrimitiveType() -> if (declaration.isNullable()) null else when(declaration.getPrimitiveType()!!) {
                PrimitiveType.BYTE -> "Int8"
                PrimitiveType.BOOLEAN -> "Bool"
                PrimitiveType.CHAR -> null // TODO: implement alongside with strings
                PrimitiveType.SHORT -> "Int16"
                PrimitiveType.INT -> "Int32"
                PrimitiveType.LONG -> "Int64"
                PrimitiveType.FLOAT -> "Float"
                PrimitiveType.DOUBLE -> "Double"
            }?.type
            declaration.isUnsignedType() -> if (declaration.isNullable()) null else when(declaration.getUnsignedType()!!) {
                UnsignedType.UBYTE -> "UInt8"
                UnsignedType.USHORT -> "UInt16"
                UnsignedType.UINT -> "UInt32"
                UnsignedType.ULONG -> "UInt64"
            }.type
            else -> null
        }
    }

    private fun bridgeFor(declaration: IrType): Bridge? = SwiftCode.build {
        val cType = mapTypeToC(declaration) ?: return null
        val swiftType = mapTypeToSwift(declaration)?.let { return Bridge.AsIs(it, cType) }
                ?: "NSObject".type // FIXME: generate concrete types

        return when {
            declaration.isRegularClass -> if (declaration.isNullable()) null else Bridge.Object(swiftType, cType)
            else -> null
        }
    }
}

private val IrType.isRegularClass: Boolean
    get() = this.classOrNull?.owner?.kind == ClassKind.CLASS

private val IrType.isClass: Boolean
    get() = this.classOrNull?.owner is IrClass && !this.isPrimitiveType(false) && !this.isUnsignedType(false)

private fun CCode.Builder.asm(name: String) = rawAttribute("asm".identifier.call(name.literal))
