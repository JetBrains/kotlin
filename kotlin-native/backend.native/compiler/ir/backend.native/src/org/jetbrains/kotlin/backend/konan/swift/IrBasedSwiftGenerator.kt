/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.swift

import org.jetbrains.kotlin.backend.jvm.ir.propertyIfAccessor
import org.jetbrains.kotlin.backend.konan.llvm.KonanBinaryInterface
import org.jetbrains.kotlin.backend.konan.llvm.isVoidAsReturnType
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name

/**
 * Generate a Swift API file for the given Kotlin IR module.
 *
 * A temporary solution to kick-start the work on Swift Export.
 * A proper solution is likely to be FIR-based and will be added later
 * as it requires a bit more work.
 *
 */
class IrBasedSwiftGenerator : IrElementVisitorVoid {
    companion object {
        private val initRuntimeIfNeeded = CCode.build { function(void, "initRuntimeIfNeeded") }

        private val switchThreadStateToNative = CCode.build { function(void, "switchThreadStateToNative") }

        private val switchThreadStateToRunnable = CCode.build { function(void, "switchThreadStateToRunnable") }

        private val bridgeFromKotlin = SwiftCode.build {
            val T = "T".type
            function(
                    "bridgeFromKotlin",
                    parameters = listOf(parameter(parameterName = "obj", type = "UnsafeMutableRawPointer".type)),
                    genericTypes = listOf(T.name.genericParameter(constraint = "AnyObject".type)),
                    returnType = T,
            )
        }

        private val bridgeToKotlin = SwiftCode.build {
            val T = "T".type
            function(
                    "bridgeToKotlin",
                    parameters = listOf(parameter(parameterName = "obj", type = T), parameter(argumentName = "slot", type = "UnsafeMutableRawPointer".type)),
                    genericTypes = listOf(T.name.genericParameter(constraint = "AnyObject".type)),
                    returnType = "UnsafeMutableRawPointer".type,
            )
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

        data class Object(override val swiftType: SwiftCode.Type, override val cType: CCode.Type) : Bridge {
            override fun from(expr: SwiftCode.Expression, delegate: BridgeCodeGenDelegate) = SwiftCode.build {
                bridgeFromKotlin.name.identifier.call(expr)
            }

            override fun into(expr: SwiftCode.Expression, delegate: BridgeCodeGenDelegate) = SwiftCode.build {
                bridgeToKotlin.name.identifier.call(expr, "slot" of delegate.getNextSlot())
            }
        }

        data class AsIs(override val swiftType: SwiftCode.Type, override val cType: CCode.Type) : Bridge {
            override fun from(expr: SwiftCode.Expression, delegate: BridgeCodeGenDelegate) = expr
            override fun into(expr: SwiftCode.Expression, delegate: BridgeCodeGenDelegate) = expr
        }
    }

    private data class Names(val swift: String, val c: String, val path: List<String>, val symbol: String) {
        companion object {
            operator fun invoke(declaration: IrFunction): Names {
                val symbolName = "_" + with(KonanBinaryInterface) { declaration.symbolName }
                val path: List<String>
                val cName: String
                val swiftName: String

                val property = declaration.propertyIfAccessor.takeIf { declaration.isPropertyAccessor }
                when {
                    property != null -> {
                        swiftName = property.getNameWithAssert().identifier
                        path = property.parent.kotlinFqName.pathSegments().map { it.identifier } + listOf(swiftName)
                        val pathString = path.joinToString(separator = "_")
                        when {
                            declaration.isGetter -> cName = "__kn_get_$pathString"
                            declaration.isSetter -> cName = "__kn_set_$pathString"
                            else -> error("A property accessor is expected to either be a setter or getter")
                        }
                    }
                    else -> {
                        swiftName = declaration.name.identifier
                        path = declaration.kotlinFqName.pathSegments().map { it.identifier }
                        val pathString = path.joinToString(separator = "_")
                        cName = "__kn_$pathString"
                    }
                }

                return Names(swiftName, cName, path, symbolName)
            }
        }
    }

    private data class Namespace<T>(
            val name: String,
            val elements: MutableList<T> = mutableListOf(),
            val children: MutableMap<String, Namespace<T>> = mutableMapOf(),
    ) {
        fun <R> reduce(transform: (List<String>, List<T>, List<R>) -> R): R {
            fun reduceFrom(node: Namespace<T>, rootPath: List<String> = emptyList(), transform: (List<String>, List<T>, List<R>) -> R): R =
                    transform(rootPath + node.name, node.elements, node.children.map { reduceFrom(it.value, rootPath + node.name, transform) })
            return reduceFrom(this, emptyList(), transform)
        }

        fun insert(path: List<String>, value: T) {
            if (path.isEmpty()) {
                elements.add(value)
                return
            }

            val key = path.first()
            val next = children.getOrPut(key) { Namespace<T>(key) }
            next.insert(path.drop(1), value)
        }
    }

    private val swiftImports = mutableListOf<SwiftCode.Import>(SwiftCode.Import.Module("Foundation"))
    private val swiftDeclarations = Namespace("", elements = mutableListOf<SwiftCode.Declaration>())

    // FIXME: we shouldn't manually generate c headers for our existing code, but here we are.
    private val cImports = CCode.build {
        mutableListOf<CCode>(
                include("stdint.h"),
        )
    }
    private val cDeclarations = mutableListOf<CCode>()

    fun buildSwiftShimFile() = SwiftCode.File {
        fun SwiftCode.Declaration.patchStatic() = when (this) {
            is SwiftCode.Declaration.Function -> this.copy(isStatic = true)
            is SwiftCode.Declaration.Variable -> when (this) {
                is SwiftCode.Declaration.StoredVariable -> this.copy(isStatic = true)
                is SwiftCode.Declaration.ComputedVariable -> this.copy(isStatic = true)
                is SwiftCode.Declaration.Constant -> this.copy(isStatic = true)
            }
            else -> this
        }

        data class Declarations(val inline: List<SwiftCode.Declaration>, val outline: List<SwiftCode.Declaration>)

        swiftImports.forEach { +it }

        swiftDeclarations.reduce<Declarations> { path, elements, children ->
            val namePath = path.dropWhile { it.isEmpty() }.takeIf { it.isNotEmpty() }
            if (namePath != null) {
                val name = namePath.fold<String, SwiftCode.Type.Nominal?>(null) { ac, el -> ac?.nested(el) ?: el.type }!!

                val inline = listOf(enum(namePath.last(), visibility = public) {
                    children.forEach { it.inline.forEach { +it.patchStatic() } }
                })

                val outline = children.flatMap { it.outline } + elements.map {
                    extension(name, visibility = public) {
                        +it.patchStatic()
                    }
                }

                Declarations(inline, outline)

            } else {
                Declarations(
                        inline = children.flatMap { it.inline },
                        outline = children.flatMap { it.outline } + elements
                )
            }
        }.let {
            it.inline.forEach { +it }
            it.outline.forEach { +it }
        }
    }

    fun buildSwiftBridgingHeader() = CCode.build {
        CCode.File(cImports + pragma("clang assume_nonnull begin") + cDeclarations + pragma("clang assume_nonnull end"))
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitProperty(declaration: IrProperty) {
        val propertyNames: Names
        val bridge = bridgeFor(declaration.getter!!.returnType) ?: return

        val getter = declaration.getter!!.let {
            val names = Names(it)
            propertyNames = names
            generateCFunction(it, names)?.also(cDeclarations::add) ?: return
            val swift = generateSwiftFunction(it, names) ?: return

            check(swift.parameters.isEmpty())
            check(swift.genericTypes.isEmpty())
            check(swift.genericTypeConstraints.isEmpty())
            checkNotNull(swift.code)

            SwiftCode.build {
                get(swift.code)
            }
        }
        val setter = declaration.setter?.let {
            val names = Names(it)
            generateCFunction(it, names)?.also(cDeclarations::add) ?: return
            val swift = generateSwiftFunction(it, names) ?: return

            check(swift.parameters.size == 1)
            check(swift.genericTypes.isEmpty())
            check(swift.genericTypeConstraints.isEmpty())
            checkNotNull(swift.code)

            SwiftCode.build {
                set(null, swift.code)
            }
        }

        swiftDeclarations.insert(propertyNames.path.dropLast(1), SwiftCode.build {
            `var`(propertyNames.swift, type = bridge.swiftType, get = getter, set = setter)
        })
    }

    override fun visitFunction(declaration: IrFunction) {
        if (!isSupported(declaration)) {
            return
        }

        val names = Names(declaration)

        val cFunction = generateCFunction(declaration, names) ?: return
        cDeclarations.add(cFunction)

        val swiftFunction = generateSwiftFunction(declaration, names) ?: return
        swiftDeclarations.insert(names.path.dropLast(1), swiftFunction)
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
            declaration.isPrimitiveType() -> if (declaration.isNullable()) null else when (declaration.getPrimitiveType()!!) {
                PrimitiveType.BYTE -> int8()
                PrimitiveType.BOOLEAN -> bool
                PrimitiveType.CHAR -> null // TODO: implement alongside with strings
                PrimitiveType.SHORT -> int16()
                PrimitiveType.INT -> int32()
                PrimitiveType.LONG -> int64()
                PrimitiveType.FLOAT -> float
                PrimitiveType.DOUBLE -> double
            }
            declaration.isUnsignedType() -> if (declaration.isNullable()) null else when (declaration.getUnsignedType()!!) {
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
            declaration.isPrimitiveType() -> if (declaration.isNullable()) null else when (declaration.getPrimitiveType()!!) {
                PrimitiveType.BYTE -> "Int8"
                PrimitiveType.BOOLEAN -> "Bool"
                PrimitiveType.CHAR -> null // TODO: implement alongside with strings
                PrimitiveType.SHORT -> "Int16"
                PrimitiveType.INT -> "Int32"
                PrimitiveType.LONG -> "Int64"
                PrimitiveType.FLOAT -> "Float"
                PrimitiveType.DOUBLE -> "Double"
            }?.type
            declaration.isUnsignedType() -> if (declaration.isNullable()) null else when (declaration.getUnsignedType()!!) {
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

    private fun generateCFunction(declaration: IrFunction, names: Names = Names(declaration)): CCode.Declaration? = CCode.build {
        declare(function(
                returnType = mapTypeToC(declaration.returnType) ?: return null,
                name = names.c,
                arguments = declaration.explicitParameters.map {
                    variable(mapTypeToC(it.type) ?: return null, it.name.identifierOrNullIfSpecial)
                } + listOfNotNull(variable(void.pointer, "returnSlot").takeIf { declaration.hasObjectHolderParameter }),
                attributes = listOf(asm(names.symbol))
        ))
    }

    private fun generateSwiftFunction(declaration: IrFunction, names: Names = Names(declaration)): SwiftCode.Declaration.Function? = SwiftCode.build {
        fun parameterName(name: Name): String = name.identifierOrNullIfSpecial ?: "newValue".takeIf { declaration.isSetter } ?: "_"

        val returnTypeBridge = bridgeFor(declaration.returnType) ?: return null
        val parameterBridges = declaration.explicitParameters.map { parameterName(it.name) to (bridgeFor(it.type) ?: return null) }

        function(
                names.swift,
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
                        listOfNotNull(if (declaration.hasObjectHolderParameter) bridgeDelegate.getNextSlot() else null)
                ).flatten()

                if (bridgeDelegate.slotsCount > 0) {
                    "withUnsafeSlots".identifier.call(
                            "count" of bridgeDelegate.slotsCount.literal,
                            "body" of closure(parameters = listOf(closureParameter("slots"))) {
                                +returnTypeBridge.from(names.c.identifier.call(parameters), bridgeDelegate)
                            }
                    )
                } else {
                    returnTypeBridge.from(names.c.identifier.call(parameters), bridgeDelegate)
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
    }
}

private val IrType.isRegularClass: Boolean
    get() = this.classOrNull?.owner?.kind == ClassKind.CLASS

private val IrType.isClass: Boolean
    get() = this.classOrNull?.owner is IrClass && !this.isPrimitiveType(false) && !this.isUnsignedType(false)

private fun CCode.Builder.asm(name: String) = rawAttribute("asm".identifier.call(name.literal))

private val IrFunction.hasObjectHolderParameter get() = this.returnType.isClass && !this.returnType.isUnit() && !this.returnType.isVoidAsReturnType()