/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.swift

import org.jetbrains.kotlin.backend.jvm.ir.propertyIfAccessor
import org.jetbrains.kotlin.backend.konan.BinaryType
import org.jetbrains.kotlin.backend.konan.computeBinaryType
import org.jetbrains.kotlin.backend.konan.llvm.computeSymbolName
import org.jetbrains.kotlin.backend.konan.llvm.isVoidAsReturnType
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isPropertyAccessor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid

internal open class IrBasedSwiftVisitor : IrElementVisitor<Unit, IrBasedSwiftVisitor.Result> {
    companion object {
        private const val BRIDGE_FROM_KOTLIN = "bridgeFromKotlin"
        private const val BRIDGE_TO_KOTLIN = "bridgeToKotlin"
        private const val INIT_RUNTIME_IF_NEEDED = "initRuntimeIfNeeded"
        private const val SWITCH_THREAD_STATE_TO_NATIVE = "switchThreadStateToNative"
        private const val SWITCH_THREAD_STATE_TO_RUNNABLE = "switchThreadStateToRunnable"

        private val kotlinAnySwiftType = SwiftCode.build { "kotlin.Object".type }
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
                BRIDGE_FROM_KOTLIN.identifier.call(expr)
            }

            override fun into(expr: SwiftCode.Expression, delegate: BridgeCodeGenDelegate) = SwiftCode.build {
                BRIDGE_TO_KOTLIN.identifier.call(expr, "slot" of delegate.getNextSlot())
            }
        }

        sealed class Self : Bridge {
            data object Reference : Self() {
                override val swiftType get() = SwiftCode.build { "Self".type }
                override val cType: CCode.Type get() = CCode.build { void.pointer }

                override fun from(expr: SwiftCode.Expression, delegate: BridgeCodeGenDelegate) = SwiftCode.build {
                    BRIDGE_FROM_KOTLIN.identifier.call(expr)
                }

                override fun into(expr: SwiftCode.Expression, delegate: BridgeCodeGenDelegate) = SwiftCode.build {
                    BRIDGE_TO_KOTLIN.identifier.call(expr, "slot" of delegate.getNextSlot())
                }
            }

            data class Value(override val cType: CCode.Type) : Self() {
                override val swiftType get() = SwiftCode.build { "Self".type }

                override fun from(expr: SwiftCode.Expression, delegate: BridgeCodeGenDelegate) = expr
                override fun into(expr: SwiftCode.Expression, delegate: BridgeCodeGenDelegate) = expr
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
                val symbolName = "_" + declaration.computeSymbolName()
                val path: List<String>
                val cName: String
                val swiftName: String

                when {
                    declaration.isPropertyAccessor -> {
                        val property = declaration.propertyIfAccessor
                        swiftName = property.getNameWithAssert().identifier
                        path = property.parent.kotlinFqName.pathSegments().map { it.identifier } + listOf(swiftName)
                        val pathString = path.joinToString(separator = "_")
                        cName = when {
                            declaration.isGetter -> "__kn_get_$pathString"
                            declaration.isSetter -> "__kn_set_$pathString"
                            else -> error("A property accessor is expected to either be a setter or getter")
                        }
                    }
                    declaration is IrConstructor -> {
                        swiftName = "init"
                        path = declaration.kotlinFqName.pathSegments().dropLast(1).map { it.identifier } + listOf("init")
                        val pathString = path.joinToString(separator = "_")
                        cName = "__kn_$pathString"
                    }
                    else -> {
                        swiftName = declaration.name.identifier
                        val pathToFunction = declaration.kotlinFqName.pathSegments().map { it.identifier }.toMutableList()

                        declaration.extensionReceiverParameter?.type
                                ?.let { type ->
                                    if (type.isPrimitiveType()) {
                                        type.mapTypeToSwift()?.render()
                                    } else {
                                        type.getClass()?.let { invoke(it).swift }
                                    }
                                }
                                ?.takeIf { declaration.dispatchReceiverParameter == null }
                                ?.let { pathToFunction.add(pathToFunction.size - 1, it) }

                        path = pathToFunction

                        val argumentTypes = declaration.valueParameters.map {
                            it.type.classFqName
                                    .toString()
                        }
                        val pathString = (path + listOf("_WithArgTypes_") + argumentTypes)
                                .joinToString(separator = "_") { it.replace(".", "") }
                        cName = "__kn_${pathString}"
                    }
                }

                return Names(swiftName, cName, path, symbolName)
            }

            operator fun invoke(declaration: IrClass): Names {
                val swiftName = declaration.name.identifier
                val path = declaration.kotlinFqName.pathSegments().map { it.identifier }
                val pathString = path.joinToString(separator = "_")
                val cName = "__kn_class_$pathString"
                return Names(swiftName, cName, path, "")
            }
        }
    }

    data class Namespace<T>(
            val name: String,
            val kind: Kind = Kind.PACKAGE,
            val elements: MutableList<T> = mutableListOf(),
            val children: MutableMap<String, Namespace<T>> = mutableMapOf(),
    ) {
        enum class Kind {
            PACKAGE,
            TYPE,
        }

        fun <R> reduce(transform: (List<String>, Kind, List<T>, List<R>) -> R): R {
            fun reduceFrom(node: Namespace<T>, rootPath: List<String> = emptyList(), transform: (List<String>, Kind, List<T>, List<R>) -> R): R =
                    transform(rootPath + node.name, node.kind, node.elements, node.children.map { reduceFrom(it.value, rootPath + node.name, transform) })
            return reduceFrom(this, emptyList(), transform)
        }

        fun makePath(path: List<String>): Namespace<T> {
            if (path.isEmpty()) {
                return this
            }

            val key = path.first()
            val next = children.getOrPut(key) { Namespace(key, if (key.isForeignType) Kind.TYPE else Kind.PACKAGE) }
            return next.makePath(path.drop(1))
        }

        fun insertElement(path: List<String>, value: T): Namespace<T> {
            return makePath(path).also { it.elements.add(value) }
        }

        fun insertNamespace(path: List<String>, namespace: Namespace<T>): Namespace<T> {
            return makePath(path).also { it.children.put(namespace.name, namespace) }
        }

        fun merge(other: Namespace<T>) {
            check(name == other.name)
            check(kind == other.kind)

            elements.addAll(other.elements)
            other.children.forEach { children.merge(it.key, it.value) { l, r -> l.merge(r); l } }
        }
    }

    data class Result(
            val swiftImports: MutableList<SwiftCode.Import> = mutableListOf(SwiftCode.Import.Module("Foundation")),
            val swiftDeclarations: Namespace<SwiftCode.Declaration> = Namespace(""),
            // FIXME: we shouldn't manually generate c headers for our existing code, but here we are.
            val cImports: MutableList<CCode> = mutableListOf(),
            val cDeclarations: MutableList<CCode> = mutableListOf(),
    )

    override fun visitElement(element: IrElement, data: Result) {
        element.acceptChildren(this, data)
    }

    override fun visitClass(declaration: IrClass, data: Result) {
        if (!isSupported(declaration)) {
            return
        }

        val names = Names(declaration)
        val path = names.path.dropLast(1)

        val namespace = Namespace<SwiftCode.Declaration>(names.swift, Namespace.Kind.TYPE)
        data.swiftDeclarations.insertNamespace(path, namespace)

        super.visitClass(declaration, data)

        val declarations = namespace.elements.partition {
            it is SwiftCode.Declaration.Method || it is SwiftCode.Declaration.Init || it is SwiftCode.Declaration.Variable
        }

        namespace.elements.clear()
        namespace.elements.addAll(declarations.second)

        val cls = SwiftCode.build {
            `class`(
                    names.swift,
                    inheritedTypes = listOfNotNull(
                            declaration.superClass?.let { Names(it).path.type } ?: kotlinAnySwiftType
                    ),
            ) {
                declarations.first.forEach { +it }
            }
        }

        data.swiftDeclarations.insertElement(path, cls)
    }

    private fun isSupported(declaration: IrClass): Boolean {
        return declaration.visibility.isPublicAPI
                && !declaration.isFun
                && !declaration.isValue
                && !declaration.isCompanion
                && !declaration.isExpect
    }

    override fun visitProperty(declaration: IrProperty, data: Result) {
        val propertyNames: Names
        val bridge = bridgeFor(declaration.getter!!.returnType) ?: return

        val getter = declaration.getter!!.let {
            val names = Names(it)
            propertyNames = names
            generateCFunction(it, names)?.also(data.cDeclarations::add) ?: return
            val swift = generateSwiftFunction(it, names) ?: return
            val code = swift.code

            check(swift.parameters.isEmpty())
            check(swift.genericTypes.isEmpty())
            check(swift.genericTypeConstraints.isEmpty())
            checkNotNull(code)

            SwiftCode.build {
                get(code)
            }
        }
        val setter = declaration.setter?.let {
            val names = Names(it)
            generateCFunction(it, names)?.also(data.cDeclarations::add) ?: return
            val swift = generateSwiftFunction(it, names) ?: return
            val code = swift.code

            check(swift.parameters.size == 1)
            check(swift.genericTypes.isEmpty())
            check(swift.genericTypeConstraints.isEmpty())
            checkNotNull(code)

            SwiftCode.build {
                set(null, code)
            }
        }

        data.swiftDeclarations.insertElement(propertyNames.path.dropLast(1), SwiftCode.build {
            `var`(propertyNames.swift, type = bridge.swiftType, get = getter, set = setter)
        })
    }

    override fun visitFunction(declaration: IrFunction, data: Result) {
        if (!isSupported(declaration)) {
            return
        }

        val names = Names(declaration)

        val cFunction = generateCFunction(declaration, names) ?: return
        data.cDeclarations.add(cFunction)

        val swiftFunction = generateSwiftFunction(declaration, names) ?: return
        data.swiftDeclarations.insertElement(names.path.dropLast(1), swiftFunction)
    }

    private fun isSupported(declaration: IrFunction): Boolean {
        // No Kotlin-exclusive stuff
        return declaration.visibility.isPublicAPI
                && declaration.contextReceiverParametersCount == 0
                && !declaration.isExpect
                && !declaration.isInline
                && !declaration.isFakeOverride
    }

    private fun bridgeFor(declaration: IrType): Bridge? = SwiftCode.build {
        val cType = declaration.mapTypeToC() ?: return null
        val swiftType = declaration.mapTypeToSwift()

        when {
            swiftType != null -> Bridge.AsIs(swiftType, cType)
            declaration.isAny() -> {
                val type = "AnyObject".type.let { if (declaration.isNullable()) it.optional else it }
                return Bridge.Object(type, cType)
            }
            declaration.computeBinaryType() is BinaryType.Reference -> {
                // FIXME: generate particular types
                val type = kotlinAnySwiftType.let { if (declaration.isNullable()) it.optional else it }
                return Bridge.Object(type, cType)
            }
            else -> return null
        }
    }

    private fun generateCFunction(declaration: IrFunction, names: Names = Names(declaration)): CCode.Declaration? = CCode.build {
        when (declaration) {
            is IrConstructor -> declare(function(
                    returnType = void,
                    name = names.c,
                    arguments = listOf(variable(void.pointer, "this")) + declaration.explicitParameters.map {
                        variable(it.type.mapTypeToC() ?: return null, it.name.identifierOrNullIfSpecial)
                    },
                    attributes = listOf(asm(names.symbol))
            ))
            else -> declare(function(
                    returnType = declaration.returnType.mapTypeToC() ?: return null,
                    name = names.c,
                    arguments = declaration.explicitParameters.map {
                        variable(it.type.mapTypeToC() ?: return null, it.name.identifierOrNullIfSpecial)
                    } + listOfNotNull(variable(void.pointer, "returnSlot").takeIf { declaration.hasObjectHolderParameter }),
                    attributes = listOf(asm(names.symbol))
            ))
        }
    }

    private fun generateSwiftFunction(declaration: IrFunction, names: Names = Names(declaration)): SwiftCode.Declaration.Function? = SwiftCode.build {
        fun SwiftCode.ListBuilder<SwiftCode.Statement>.generateFunctionBody(parameterBridges: List<Pair<String, Bridge>>, returnTypeBridge: Bridge?) {
            val isInit = declaration is IrConstructor
            val resultBridge = returnTypeBridge ?: Bridge.AsIs(
                    "UnsafeMutableRawPointer".type,
                    CCode.build { void.pointer }
            )

            +INIT_RUNTIME_IF_NEEDED.identifier.call()
            +SWITCH_THREAD_STATE_TO_RUNNABLE.identifier.call()
            +defer { +SWITCH_THREAD_STATE_TO_NATIVE.identifier.call() }

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
                                +resultBridge.from(names.c.identifier.call(parameters), bridgeDelegate)
                            }
                    )
                } else {
                    resultBridge.from(names.c.identifier.call(parameters), bridgeDelegate)
                }
            }

            if (isInit) {
                +"super".identifier.access("init").call()
                +call
            } else {
                +`return`(call)
            }
        }

        fun parameterName(valueParameter: IrValueParameter): String = valueParameter.name.identifierOrNullIfSpecial
                ?: "newValue".takeIf { declaration.isSetter }
                ?: "receiver".takeIf { declaration.extensionReceiverParameter == valueParameter }
                ?: "_"

        val returnTypeBridge = bridgeFor(declaration.returnType) ?: return null
        val bridges = declaration.explicitParameters.map { parameterName(it) to (bridgeFor(it.type) ?: return null) }
        val parameterBridges: List<Pair<String, Bridge>>
        val argumentBridges: List<Pair<String, Bridge>>

        when {
            declaration.dispatchReceiverParameter != null || declaration.extensionReceiverParameter != null -> {
                parameterBridges = bridges.drop(1)
                argumentBridges = listOf(
                    "self" to bridges
                            .first()
                            .let {
                                if (it.second.cType is CCode.Type.Pointer)
                                    Bridge.Self.Reference
                                else
                                    Bridge.Self.Value(it.second.cType)
                            }
                ) + parameterBridges
            }

            declaration is IrConstructor -> {
                parameterBridges = bridges
                argumentBridges = listOf("self" to Bridge.Self.Reference) + parameterBridges
            }

            else -> {
                parameterBridges = bridges
                argumentBridges = bridges
            }
        }

        when {
            declaration is IrConstructor -> init(
                    parameters = parameterBridges.map { parameter(it.first, type = it.second.swiftType) },
                    isOverride = parameterBridges.isEmpty(),
                    visibility = public
            ) {
                generateFunctionBody(argumentBridges, null)
            }

            declaration.dispatchReceiverParameter != null || declaration.extensionReceiverParameter != null -> method(
                    names.swift,
                    parameters = parameterBridges.map { parameter(it.first, type = it.second.swiftType) },
                    returnType = returnTypeBridge.swiftType,
                    visibility = public
            ) {
                generateFunctionBody(argumentBridges, returnTypeBridge)
            }

            else -> function(
                    names.swift,
                    parameters = parameterBridges.map { parameter(it.first, type = it.second.swiftType) },
                    returnType = returnTypeBridge.swiftType,
                    visibility = public
            ) {
                generateFunctionBody(argumentBridges, returnTypeBridge)
            }
        }
    }
}

/**
 * Generate a Swift API file for the given Kotlin IR module.
 *
 * A temporary solution to kick-start the work on Swift Export.
 * A proper solution is likely to be FIR-based and will be added later
 * as it requires a bit more work.
 *
 */
class IrBasedSwiftGenerator : IrElementVisitorVoid {
    private val result = IrBasedSwiftVisitor.Result(
            swiftImports = mutableListOf(
                    SwiftCode.Import.Module("Foundation")
            ),
            cImports = mutableListOf(
                    CCode.Include("stdint.h")
            )
    )

    override fun visitElement(element: IrElement) {
        IrBasedSwiftVisitor().visitElement(element, result)
    }

    fun buildSwiftShimFile() = SwiftCode.File {
        fun SwiftCode.Declaration.patchStatic() = when (this) {
            is SwiftCode.Declaration.Method -> this
            is SwiftCode.Declaration.FreestandingFunction -> method(
                    name = name,
                    genericTypes = genericTypes,
                    parameters = parameters,
                    returnType = returnType,
                    isStatic = true,
                    isAsync = isAsync,
                    isThrowing = isThrowing,
                    attributes = attributes,
                    visibility = visibility,
                    genericTypeConstraints = genericTypeConstraints,
                    body = code,
            )
            is SwiftCode.Declaration.Variable -> when (this) {
                is SwiftCode.Declaration.StoredVariable -> this.copy(isStatic = true)
                is SwiftCode.Declaration.ComputedVariable -> this.copy(isStatic = true)
                is SwiftCode.Declaration.Constant -> this.copy(isStatic = true)
            }
            else -> this
        }

        data class Declarations(val inline: List<SwiftCode.Declaration>, val outline: List<SwiftCode.Declaration>)

        result.swiftImports.forEach { +it }

        result.swiftDeclarations.reduce<Declarations> { path, kind, elements, children ->
            val namePath = path.dropWhile { it.isEmpty() }.takeIf { it.isNotEmpty() }
            if (namePath != null) {
                val name = namePath.type!!

                val inline = mutableListOf<SwiftCode.Declaration>()
                val outline = mutableListOf<SwiftCode.Declaration>()

                when (kind) {
                    IrBasedSwiftVisitor.Namespace.Kind.TYPE -> {
                        outline.add(extension(name) {
                            children.forEach { it.inline.forEach { +it.patchStatic() } }
                        })
                    }
                    IrBasedSwiftVisitor.Namespace.Kind.PACKAGE -> {
                        inline.add(enum(namePath.last(), visibility = public) {
                            children.forEach { it.inline.forEach { +it.patchStatic() } }
                        })
                    }
                }

                for (child in children) {
                    outline.addAll(child.outline)
                }

                for (element in elements) {
                    outline.add(extension(name) {
                        +element.patchStatic()
                    })
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
        CCode.File(result.cImports + pragma("clang assume_nonnull begin") + result.cDeclarations + pragma("clang assume_nonnull end"))
    }

    fun preventFunctionAndVariableNameClashes() {

        fun List<SwiftCode.Declaration>.fixFunctionAndPropertyConflict() =
                filterIsInstance<SwiftCode.Declaration.Function>()
                        .filter { it !is SwiftCode.Declaration.Init }
                        .filter { it.parameters.isEmpty() }
                        .forEach(SwiftCode.Declaration.Function::addDummyParam)

        fun <T> IrBasedSwiftVisitor.Namespace<T>.collectPackages(): List<IrBasedSwiftVisitor.Namespace<T>> = children.values.filter { it.kind == IrBasedSwiftVisitor.Namespace.Kind.PACKAGE }
        fun <T> IrBasedSwiftVisitor.Namespace<T>.collectClasses(): List<SwiftCode.Declaration.Class> = elements.filterIsInstance<SwiftCode.Declaration.Class>()
        fun SwiftCode.Declaration.Class.fixFunctionAndPropertiesConflict() = block.elements.fixFunctionAndPropertyConflict()
        fun SwiftCode.Declaration.Class.recursivelyFixFunctionAndPropertyConflict(currentLvl: IrBasedSwiftVisitor.Namespace<SwiftCode.Declaration>) {
            fixFunctionAndPropertiesConflict()

            val nextLvl = currentLvl.children[name] ?: return
            val innerClasses = nextLvl.collectClasses()
            innerClasses.forEach {
                it.recursivelyFixFunctionAndPropertyConflict(nextLvl)
            }
        }

        fun IrBasedSwiftVisitor.Namespace<SwiftCode.Declaration>.recursivelyFixConflicts() {
            elements.fixFunctionAndPropertyConflict()
            val topClasses = collectClasses()
            topClasses.forEach { it.recursivelyFixFunctionAndPropertyConflict(this) }

            collectPackages().forEach(IrBasedSwiftVisitor.Namespace<SwiftCode.Declaration>::recursivelyFixConflicts)
        }

        val topPackage = result.swiftDeclarations
        topPackage.recursivelyFixConflicts()
    }
}

private fun SwiftCode.Declaration.Function.addDummyParam() {
    val newParams = parameters.toMutableList()
    newParams.add(
            0,
            SwiftCode.Declaration.Function.Parameter(
                    null,
                    "dummy",
                    SwiftCode.Type.Named("Void"),
                    SwiftCode.Expression.Identifier("()")))
    applyNewParameters(newParams)
}

private val IrType.isRegularClass: Boolean
    get() = this.classOrNull?.owner?.kind == ClassKind.CLASS

private val IrType.isClass: Boolean
    get() = this.classOrNull?.owner is IrClass && !this.isPrimitiveType(false) && !this.isUnsignedType(false)

private fun CCode.Builder.asm(name: String) = rawAttribute("asm".identifier.call(name.literal))

private val IrFunction.hasObjectHolderParameter get() = this !is IrConstructor && this.returnType.isClass && !this.returnType.isUnit() && !this.returnType.isVoidAsReturnType()

private fun IrType.mapTypeToC(): CCode.Type? = CCode.build {
    when {
        isUnit() -> if (isNullable()) null else void
        isPrimitiveType() -> if (isNullable()) null else when (getPrimitiveType()!!) {
            PrimitiveType.BYTE -> int8()
            PrimitiveType.BOOLEAN -> bool
            PrimitiveType.CHAR -> null // TODO: implement alongside with strings
            PrimitiveType.SHORT -> int16()
            PrimitiveType.INT -> int32()
            PrimitiveType.LONG -> int64()
            PrimitiveType.FLOAT -> float
            PrimitiveType.DOUBLE -> double
        }
        isUnsignedType() -> if (isNullable()) null else when (getUnsignedType()!!) {
            UnsignedType.UBYTE -> int8(isUnsigned = true)
            UnsignedType.USHORT -> int16(isUnsigned = true)
            UnsignedType.UINT -> int32(isUnsigned = true)
            UnsignedType.ULONG -> int64(isUnsigned = true)
        }
        isRegularClass -> void.pointer(nullability = CCode.Type.Pointer.Nullability.NULLABLE.takeIf { isNullable() })
        else -> null
    }
}

private fun IrType.mapTypeToSwift(): SwiftCode.Type? = SwiftCode.build {
    when {
        isNullable() -> null
        isUnit() -> "Void".type
        isPrimitiveType() -> when (getPrimitiveType()!!) {
            PrimitiveType.BYTE -> "Int8"
            PrimitiveType.BOOLEAN -> "Bool"
            PrimitiveType.CHAR -> null // TODO: implement alongside with strings
            PrimitiveType.SHORT -> "Int16"
            PrimitiveType.INT -> "Int32"
            PrimitiveType.LONG -> "Int64"
            PrimitiveType.FLOAT -> "Float"
            PrimitiveType.DOUBLE -> "Double"
        }?.type
        isUnsignedType() -> when (getUnsignedType()!!) {
            UnsignedType.UBYTE -> "UInt8"
            UnsignedType.USHORT -> "UInt16"
            UnsignedType.UINT -> "UInt32"
            UnsignedType.ULONG -> "UInt64"
        }.type
        else -> null
    }
}

private val String.isForeignType: Boolean
    get() = when (this) {
        "Int8",
        "Bool",
        "Int16",
        "Int32",
        "Int64",
        "Float",
        "Double",
        "UInt8",
        "UInt16",
        "UInt32",
        "UInt64" -> true // TODO: foreign types are more complex, the detection of them should be rewritten into actual detection
        else -> false
    }
