/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.serialization.kotlinLibrary
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.ir.annotations.Escapes
import org.jetbrains.kotlin.backend.konan.ir.annotations.PointsTo
import org.jetbrains.kotlin.backend.konan.ir.annotations.escapes
import org.jetbrains.kotlin.backend.konan.ir.annotations.pointsTo
import org.jetbrains.kotlin.backend.konan.ir.hasFinalizer
import org.jetbrains.kotlin.backend.konan.ir.implementedInterfaces
import org.jetbrains.kotlin.backend.konan.ir.isAbstract
import org.jetbrains.kotlin.backend.konan.ir.isBuiltInOperator
import org.jetbrains.kotlin.backend.konan.llvm.computeFunctionName
import org.jetbrains.kotlin.backend.konan.llvm.computeSymbolName
import org.jetbrains.kotlin.backend.konan.llvm.computeTypeInfoSymbolName
import org.jetbrains.kotlin.backend.konan.llvm.isExported
import org.jetbrains.kotlin.backend.konan.llvm.localHash
import org.jetbrains.kotlin.backend.konan.lower.bridgeTarget
import org.jetbrains.kotlin.backend.konan.lower.getDefaultValueForOverriddenBuiltinFunction
import org.jetbrains.kotlin.backend.konan.lower.isEagerStaticInitializer
import org.jetbrains.kotlin.backend.konan.util.CustomBitSet
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.objcinterop.getExternalObjCMethodInfo
import org.jetbrains.kotlin.ir.objcinterop.isObjCClass
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.library.metadata.FORWARD_DECLARATIONS_MODULE_NAME
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary

internal object DataFlowIR {
    // Public types and functions, as well as fields, are identified across modules by 64-bit hashes
    // (see [SymbolTable.mapClassReferenceType], [SymbolTable.mapFunction] and [SymbolTable.mapField]).
    // The names are saved along with the hashes and serve as an independent witness:
    // whenever two entities have clashing hashes, their names must be the same.
    fun checkNoHashCollision(kind: String, hash: Long, name: String?, otherName: String?) {
        if (name != null && otherName != null && name != otherName)
            error("Hash collision between $kind '$name' and '$otherName': $hash")
    }

    abstract class Type(
            val index: Int,
            val isFinal: Boolean,
            val isAbstract: Boolean,
            val hasFinalizer: Boolean,
            val module: Module?,
            // Types deserialized from a cache are created without IR; the declaration is attached
            // later, when the type gets resolved from IR (see SymbolTable.mapClassReferenceType).
            var irClass: IrClass?,
            val name: String?
    ) {
        val superTypes = mutableListOf<Type>()
        val vtable = mutableListOf<FunctionSymbol>()
        val itable = mutableMapOf<Int, List<FunctionSymbol>>()

        // Special marker type forbidding devirtualization on its instances.
        object Virtual : Type(0, false, true, false, null, null, "\$VIRTUAL")

        class Public(
                val hash: Long,
                index: Int,
                isFinal: Boolean,
                isAbstract: Boolean,
                hasFinalizer: Boolean,
                module: Module,
                irClass: IrClass?,
                name: String? = null,
                // The declaration belongs to no DFG-carrying module (a cinterop library's class, a lazily generated cinterop stub,
                // a lowering-synthesized class like a function-interface class) and is compiled into every module referencing it -
                // like a C/C++ inline function in a header. Several DFGs can publish a copy of it (all are interchangeable).
                val isLinkOnce: Boolean = false,
        ) : Type(index, isFinal, isAbstract, hasFinalizer, module, irClass, name) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Public) return false

                return hash == other.hash
            }

            override fun hashCode(): Int {
                return hash.hashCode()
            }

            override fun toString(): String {
                return "PublicType(hash='$hash', index='$index', name='$name')"
            }
        }

        class Private(index: Int, isFinal: Boolean, isAbstract: Boolean, hasFinalizer: Boolean,
                      module: Module, irClass: IrClass?, name: String? = null)
            : Type(index, isFinal, isAbstract, hasFinalizer, module, irClass, name) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Private) return false

                return index == other.index
            }

            override fun hashCode(): Int {
                return index
            }

            override fun toString(): String {
                return "PrivateType(index=$index, name='$name')"
            }
        }
    }

    class Module

    object FunctionAttributes {
        val IS_STATIC_FIELD_INITIALIZER = 1
        val RETURNS_UNIT = 4
        val RETURNS_NOTHING = 8
        val EXPLICITLY_EXPORTED = 16
        val IS_EAGER_STATIC_INITIALIZER = 32
        val IS_BRIDGE = 64
    }

    class FunctionParameter(val type: Type)

    abstract class FunctionSymbol(val attributes: Int, irDeclaration: IrDeclaration?, val name: String?) {
        // A symbol deserialized from a cache is created without IR; the declaration is attached later
        // if it turns out to be a link-once one referenced from IR (see SymbolTable.mapFunction).
        var irDeclaration: IrDeclaration? = irDeclaration
            set(value) {
                checkDeclaration(value)
                field = value
            }

        init {
            checkDeclaration(irDeclaration)
        }

        private fun checkDeclaration(declaration: IrDeclaration?) {
            require(declaration == null || declaration is IrSimpleFunction || declaration is IrField) {
                "Unexpected declaration: ${declaration?.render()}"
            }
        }

        lateinit var parameters: Array<FunctionParameter>
        lateinit var returnParameter: FunctionParameter

        // The type of the class this function is declared in, if any.
        var declaringType: Type? = null

        val isStaticFieldInitializer = attributes.and(FunctionAttributes.IS_STATIC_FIELD_INITIALIZER) != 0
        val returnsUnit = attributes.and(FunctionAttributes.RETURNS_UNIT) != 0
        val returnsNothing = attributes.and(FunctionAttributes.RETURNS_NOTHING) != 0
        val explicitlyExported = attributes.and(FunctionAttributes.EXPLICITLY_EXPORTED) != 0
        val isEagerStaticInitializer = attributes.and(FunctionAttributes.IS_EAGER_STATIC_INITIALIZER) != 0
        val isBridge = attributes.and(FunctionAttributes.IS_BRIDGE) != 0

        val irFunction: IrSimpleFunction? get() = irDeclaration as? IrSimpleFunction
        val irFile: IrFile? get() = irDeclaration?.fileOrNull

        class External(
                val hash: Long,
                attributes: Int,
                irDeclaration: IrDeclaration?,
                name: String?,
                val isExported: Boolean,
                val escapes: Escapes?,
                val pointsTo: PointsTo?,
        ) : FunctionSymbol(attributes, irDeclaration, name) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is External) return false

                return hash == other.hash
            }

            override fun hashCode(): Int {
                return hash.hashCode()
            }

            override fun toString(): String {
                return "ExternalFunction(hash='$hash', name='$name', escapes='$escapes', pointsTo='$pointsTo')"
            }
        }

        abstract class Declared(
                val module: Module, attributes: Int, irDeclaration: IrDeclaration?, var bridgeTarget: FunctionSymbol?, name: String?
        ) : FunctionSymbol(attributes, irDeclaration, name)

        class Public(
                val hash: Long,
                module: Module,
                attributes: Int,
                irDeclaration: IrDeclaration?,
                bridgeTarget: FunctionSymbol?,
                name: String? = null,
                // See similar [Type.Public] field.
                val isLinkOnce: Boolean = false,
        ) : Declared(module, attributes, irDeclaration, bridgeTarget, name) {

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Public) return false

                return hash == other.hash
            }

            override fun hashCode(): Int {
                return hash.hashCode()
            }

            override fun toString(): String {
                return "PublicFunction(hash='$hash', name='$name')"
            }
        }

        class Private(val index: Int, module: Module,
                      attributes: Int, irDeclaration: IrDeclaration?, bridgeTarget: FunctionSymbol?, name: String? = null)
            : Declared(module, attributes, irDeclaration, bridgeTarget, name) {

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Private) return false

                return index == other.index
            }

            override fun hashCode(): Int {
                return index
            }

            override fun toString(): String {
                return "PrivateFunction(index=$index, name='$name')"
            }
        }
    }

    class Field(val type: Type, val hash: Long, val name: String? = null) {
        override fun toString() = "Field(type=$type, hash=$hash, name=$name)"
    }

    class Edge(val castToType: Type?) {

        lateinit var node: Node

        constructor(node: Node, castToType: Type?) : this(castToType) {
            this.node = node
        }
    }

    enum class VariableKind {
        Ordinary,
        Temporary,
        CatchParameter
    }

    sealed class Node {
        class Parameter(val index: Int) : Node()

        open class Const(val type: Type) : Node()

        class SimpleConst<T : Any>(type: Type, val value: T) : Const(type)

        object Null : Node()

        open class Call(val callee: FunctionSymbol, val arguments: List<Edge>, val returnType: Type,
                        val irCallSite: IrCall?) : Node()

        class StaticCall(callee: FunctionSymbol, arguments: List<Edge>, returnType: Type, irCallSite: IrCall?)
            : Call(callee, arguments, returnType, irCallSite)

        sealed class VirtualCall(callee: FunctionSymbol, arguments: List<Edge>,
                                 val receiverType: Type, returnType: Type, irCallSite: IrCall?)
            : Call(callee, arguments, returnType, irCallSite)

        class VtableCall(callee: FunctionSymbol, receiverType: Type, val calleeVtableIndex: Int,
                         arguments: List<Edge>, returnType: Type, irCallSite: IrCall?)
            : VirtualCall(callee, arguments, receiverType, returnType, irCallSite)

        class ItableCall(callee: FunctionSymbol, receiverType: Type, val interfaceId: Int, val calleeVtableIndex: Int,
                         arguments: List<Edge>, returnType: Type, irCallSite: IrCall?)
            : VirtualCall(callee, arguments, receiverType, returnType, irCallSite)

        class Singleton(val type: Type, val constructor: FunctionSymbol?, val arguments: List<Edge>?) : Node()

        sealed class Alloc(val type: Type, val irCallSite: IrCall?) : Node()

        class AllocInstance(type: Type, irCallSite: IrCall?) : Alloc(type, irCallSite)

        class AllocArray(type: Type, val size: Edge, irCallSite: IrCall?) : Alloc(type, irCallSite)

        class FunctionReference(val symbol: FunctionSymbol, val type: Type, val returnType: Type) : Node()

        class FieldRead(val receiver: Edge?, val field: Field, val type: Type, val ir: IrGetField?) : Node()

        class FieldWrite(val receiver: Edge?, val field: Field, val value: Edge) : Node()

        class ArrayRead(val callee: FunctionSymbol, val array: Edge, val index: Edge, val type: Type, val irCallSite: IrCall?) : Node()

        class ArrayWrite(val callee: FunctionSymbol, val array: Edge, val index: Edge, val value: Edge, val type: Type) : Node()

        class SaveCoroutineState(liveVariables: List<Variable>) : Node() {
            val liveVariables = mutableListOf<Variable>().also { it += liveVariables }
        }

        class Variable(values: List<Edge>, val type: Type, val kind: VariableKind) : Node() {
            val values = mutableListOf<Edge>().also { it += values }
        }

        class Scope(val depth: Int, nodes: List<Node>) : Node() {
            val nodes = mutableSetOf<Node>().also { it += nodes }
        }
    }

    // Note: scopes form a tree.
    class FunctionBody(val rootScope: Node.Scope, val allScopes: List<Node.Scope>,
                       val returns: Node.Variable, val throws: Node.Variable) {
        inline fun forEachNonScopeNode(block: (Node) -> Unit) {
            for (scope in allScopes)
                for (node in scope.nodes)
                    if (node !is Node.Scope)
                        block(node)
        }
    }

    class Function(val symbol: FunctionSymbol, val body: FunctionBody) {

        fun debugOutput() = println(debugString())

        fun debugString() = buildString {
            appendLine("FUNCTION $symbol")
            appendLine("Params: ${symbol.parameters.contentToString()}")
            val nodes = listOf(body.rootScope) + body.allScopes.flatMap { it.nodes }
            val ids = nodes.withIndex().associateBy({ it.value }, { it.index })
            nodes.forEach {
                appendLine("    NODE #${ids[it]!!}")
                appendLine(nodeToString(it, ids))
            }
            appendLine("    RETURNS")
            append(nodeToString(body.returns, ids))
        }

        companion object {
            fun printNode(node: Node, ids: Map<Node, Int>) = print(nodeToString(node, ids))

            fun nodeToString(node: Node, ids: Map<Node, Int>) = when (node) {
                is Node.Const ->
                    "        CONST ${node.type}"

                Node.Null ->
                    "        NULL"

                is Node.Parameter ->
                    "        PARAM ${node.index}"

                is Node.Singleton ->
                    "        SINGLETON ${node.type}"

                is Node.AllocInstance ->
                    "        ALLOC INSTANCE ${node.type}"

                is Node.AllocArray ->
                    "        ALLOC ARRAY ${node.type} of size #${ids[node.size.node]!!}"

                is Node.FunctionReference ->
                    "        FUNCTION REFERENCE ${node.symbol}"

                is Node.StaticCall -> buildString {
                    append("        STATIC CALL ${node.callee}. Return type = ${node.returnType}")
                    appendList(node.arguments) {
                        append("            ARG #${ids[it.node]!!}")
                        appendCastTo(it.castToType)
                    }
                }

                is Node.VtableCall -> buildString {
                    appendLine("        VIRTUAL CALL ${node.callee}. Return type = ${node.returnType}")
                    appendLine("            RECEIVER: ${node.receiverType}")
                    append("            VTABLE INDEX: ${node.calleeVtableIndex}")
                    appendList(node.arguments) {
                        append("            ARG #${ids[it.node]!!}")
                        appendCastTo(it.castToType)
                    }
                }

                is Node.ItableCall -> buildString {
                    appendLine("        INTERFACE CALL ${node.callee}. Return type = ${node.returnType}")
                    appendLine("            RECEIVER: ${node.receiverType}")
                    append("            INTERFACE ID: ${node.interfaceId}. ITABLE INDEX: ${node.calleeVtableIndex}")
                    appendList(node.arguments) {
                        append("            ARG #${ids[it.node]!!}")
                        appendCastTo(it.castToType)
                    }
                }

                is Node.FieldRead -> buildString {
                    appendLine("        FIELD READ ${node.field}")
                    append("            RECEIVER #${node.receiver?.node?.let { ids[it]!! } ?: "null"}")
                    appendCastTo(node.receiver?.castToType)
                }

                is Node.FieldWrite -> buildString {
                    appendLine("        FIELD WRITE ${node.field}")
                    append("            RECEIVER #${node.receiver?.node?.let { ids[it]!! } ?: "null"}")
                    appendCastTo(node.receiver?.castToType)
                    appendLine()
                    append("            VALUE #${ids[node.value.node]!!}")
                    appendCastTo(node.value.castToType)
                }

                is Node.ArrayRead -> buildString {
                    appendLine("        ARRAY READ")
                    append("            ARRAY #${ids[node.array.node]}")
                    appendCastTo(node.array.castToType)
                    appendLine()
                    append("            INDEX #${ids[node.index.node]!!}")
                    appendCastTo(node.index.castToType)
                }

                is Node.ArrayWrite -> buildString {
                    appendLine("        ARRAY WRITE")
                    append("            ARRAY #${ids[node.array.node]}")
                    appendCastTo(node.array.castToType)
                    appendLine()
                    append("            INDEX #${ids[node.index.node]!!}")
                    appendCastTo(node.index.castToType)
                    appendLine()
                    append("            VALUE #${ids[node.value.node]!!}")
                    appendCastTo(node.value.castToType)
                }

                is Node.SaveCoroutineState -> buildString {
                    appendLine("        SAVE COROUTINE STATE")
                    appendList(node.liveVariables) {
                        append("            VAL #${ids[it]!!}")
                    }
                }

                is Node.Variable -> buildString {
                    append("       ${node.kind}")
                    appendList(node.values) {
                        append("            VAL #${ids[it.node]!!}")
                        appendCastTo(it.castToType)
                    }
                }

                is Node.Scope -> buildString {
                    append("       SCOPE ${node.depth}")
                    appendList(node.nodes.toList()) {
                        append("            SUBNODE #${ids[it]!!}")
                    }
                }

                else -> "        UNKNOWN: ${node::class.java}"
            }

            private fun <T> StringBuilder.appendList(list: List<T>, itemPrinter: StringBuilder.(T) -> Unit) {
                if (list.isEmpty()) return
                for (i in list.indices) {
                    appendLine()
                    itemPrinter(list[i])
                }
            }

            private fun StringBuilder.appendCastTo(type: Type?) {
                if (type != null)
                    append(" CASTED TO ${type}")
            }
        }
    }

    class TypeHierarchy(val allTypes: Array<Type>) {
        private val typesSubTypes = Array(allTypes.size) { mutableListOf<Type>() }
        private val allInheritors = Array(allTypes.size) { CustomBitSet() }

        init {
            val visited = CustomBitSet()

            fun processType(type: Type) {
                if (visited[type.index]) return
                visited.set(type.index)
                type.superTypes
                        .forEach { superType ->
                            val subTypes = typesSubTypes[superType.index]
                            subTypes += type
                            processType(superType)
                        }
            }

            allTypes.forEach { processType(it) }
        }

        fun inheritorsOf(type: Type): CustomBitSet {
            val typeId = type.index
            val inheritors = allInheritors[typeId]
            if (!inheritors.isEmpty || type == Type.Virtual) return inheritors
            inheritors.set(typeId)
            for (subType in typesSubTypes[typeId])
                inheritors.or(inheritorsOf(subType))
            return inheritors
        }
    }

    class SymbolTable(
            val context: NativeBackendContext,
            val llvmModuleSpecification: LlvmModuleSpecification,
            val module: Module,
    ) {
        private val TAKE_NAMES = true // Take fqNames for all functions and types (for debug purposes).

        private inline fun takeName(block: () -> String) = if (TAKE_NAMES) block() else null

        var typeIndex = 1 // 0 for [Virtual]
        var privateFunIndex = 0

        val primitiveTypes: List<Type> = PrimitiveBinaryType.entries.map { mapPrimitiveBinaryType(it) }

        // Public types and functions of the other modules, deserialized from their caches' DFGs and published by hash.
        val externalPublicTypes = mutableMapOf<Long, Type.Public>()
        val externalPublicFunctions = mutableMapOf<Long, FunctionSymbol.Public>()

        private var sealed = false
        val typesList = mutableListOf<Type>()
        val functionsList = mutableListOf<FunctionSymbol>()
        val classMap = mutableMapOf<IrClass, Type>()
        val functionMap = mutableMapOf<IrDeclaration, FunctionSymbol>()
        val fieldMap = mutableMapOf<IrField, Field>()

        // The names of all the [FunctionSymbol.External]s by hash - these symbols are created by every
        // module referencing the function and are unified solely by hash.
        private val externalFunctionNames = mutableMapOf<Long, String>()

        fun checkExternalFunction(hash: Long, name: String?) {
            if (name != null)
                checkNoHashCollision("external functions", hash, externalFunctionNames.getOrPut(hash) { name }, name)
        }

        val typeHierarchy by lazy {
            require(sealed) { "The symbol table has been sealed" }

            val allDeclaredTypes = listOf(Type.Virtual) + primitiveTypes + typesList
            val allTypes = Array<Type>(allDeclaredTypes.size) { Type.Virtual }
            for (type in allDeclaredTypes)
                allTypes[type.index] = type

            TypeHierarchy(allTypes)
        }

        fun populateWith(irModule: IrModuleFragment) {
            irModule.accept(object : IrVisitorVoid() {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                    declaration.body?.let { mapFunction(declaration) }
                }

                override fun visitField(declaration: IrField) {
                    if (declaration.parent is IrFile)
                        declaration.initializer?.let {
                            mapFunction(declaration)
                        }
                }

                override fun visitClass(declaration: IrClass) {
                    declaration.acceptChildrenVoid(this)

                    mapClassReferenceType(declaration)
                }
            }, data = null)
        }

        fun seal() {
            sealed = true
        }

        fun mapField(field: IrField): Field = fieldMap.getOrPut(field) {
            val name = field.name.asString()
            Field(
                    mapType(field.type),
                    localHash("${field.fqNameForIrSerialization}.$name".toByteArray()),
                    takeName { name }
            )
        }

        fun mapClassReferenceType(irClass: IrClass): Type {
            classMap[irClass]?.let { return it }

            // Do not try to devirtualize ObjC classes.
            if (irClass.moduleFragment.name == FORWARD_DECLARATIONS_MODULE_NAME || irClass.isObjCClass())
                return Type.Virtual

            val isFinal = irClass.isFinalClass
            val isAbstract = irClass.isAbstract()
            val name = irClass.fqNameForIrSerialization.asString()
            // A public type is identified across modules by the hash of its binary name - the very name
            // the modules get linked together by, so it is unique and is computed the same way by all of them.
            fun computeHash() = localHash(irClass.computeTypeInfoSymbolName().toByteArray())

            // Link-once declarations (see [Type.Public.isLinkOnce]) belong to no DFG-carrying module, so they aren't guaranteed
            // to be resolvable by hash and are mapped by each module referencing them (see below).
            val isLinkOnce = irClass.moduleFragment.kotlinLibrary.let {
                it == null || it.isCInteropLibrary() // cinterop libraries' caches don't have DFGs
            }
            if (!isLinkOnce && !llvmModuleSpecification.containsDeclaration(irClass)) {
                // A non-exported class of another module isn't published by that module's DFG. It can't be
                // legitimately used from here, but still can be mentioned in a signature: partial linkage
                // replaces the usages of a no longer accessible class with linkage errors but leaves the types.
                // Nothing is known about such a class.
                if (!irClass.isExported) return Type.Virtual

                // Attach the IR declaration to the deserialized type - the analyses might still need it.
                val hash = computeHash()
                return externalPublicTypes[hash]
                        ?.also {
                            checkNoHashCollision("types", hash, it.name, name)
                            it.irClass = irClass
                        }
                        ?: error("Class ${irClass.render()} is not found")
            }

            // A cached library referencing a link-once class has published its own copy of the class -
            // reuse it instead of duplicating.
            if (isLinkOnce && irClass.isExported) {
                val hash = computeHash()
                externalPublicTypes[hash]?.takeIf { it.isLinkOnce }?.let {
                    checkNoHashCollision("types", hash, it.name, name)
                    it.irClass = irClass
                    classMap[irClass] = it
                    return it
                }
            }

            require(!sealed) { "The symbol table has been sealed. irClass = ${irClass.render()}" }
            val type = if (irClass.isExported)
                Type.Public(computeHash(), typeIndex++, isFinal, isAbstract, irClass.hasFinalizer,
                        module, irClass, takeName { name }, isLinkOnce)
            else
                Type.Private(typeIndex++, isFinal, isAbstract, irClass.hasFinalizer,
                        module, irClass, takeName { name })

            classMap[irClass] = type
            typesList.add(type)

            type.superTypes += irClass.superTypes.map { mapClassReferenceType(it.getClass()!!) }
            if (!isAbstract) {
                val layoutBuilder = context.getLayoutBuilder(irClass)
                type.vtable += layoutBuilder.vtableEntries.map {
                    val implementation = it.getImplementation(context)
                            ?: error(
                                    irClass.fileOrNull,
                                    irClass,
                                    """
                                        no implementation found for ${it.overriddenFunction.render()}
                                        when building vtable for ${irClass.render()}
                                    """.trimIndent()
                            )

                    mapFunction(implementation)
                }
                val interfaces = irClass.implementedInterfaces.map { context.getLayoutBuilder(it) }
                for (iface in interfaces) {
                    type.itable[iface.classId] = iface.interfaceVTableEntries.map {
                        val implementation = layoutBuilder.overridingOf(it)
                                ?: error(
                                        irClass.fileOrNull,
                                        irClass,
                                        """
                                            no implementation found for ${it.render()}
                                            when building itable for ${iface.irClass.render()}
                                            implementation in ${irClass.render()}
                                        """.trimIndent()
                                )

                        mapFunction(implementation)
                    }
                }
            } else if (irClass.isInterface) {
                // Warmup interface table so it is computed before DCE.
                context.getLayoutBuilder(irClass).interfaceVTableEntries
            }
            return type
        }

        private fun choosePrimary(erasure: List<IrClass>): IrClass {
            if (erasure.size == 1) return erasure[0]
            // A parameter with constraints - choose class if exists.
            return erasure.singleOrNull { !it.isInterface } ?: context.irBuiltIns.anyClass.owner
        }

        private fun mapPrimitiveBinaryType(primitiveBinaryType: PrimitiveBinaryType): Type =
                Type.Public(
                        primitiveBinaryType.ordinal.toLong(),
                        typeIndex++,
                        true,
                        false,
                        false,
                        module,
                        null,
                        takeName { primitiveBinaryType.name }
                )

        fun mapType(type: IrType) = when (val binaryType = type.computeBinaryType()) {
            is BinaryType.Primitive -> primitiveTypes[binaryType.type.ordinal]
            is BinaryType.Reference -> mapClassReferenceType(choosePrimary(binaryType.types.toList()))
        }

        fun mapFunction(declaration: IrDeclaration): FunctionSymbol = when (declaration) {
            is IrSimpleFunction -> mapFunction(declaration)
            is IrField -> mapPropertyInitializer(declaration)
            else -> error("Unknown declaration: $declaration")
        }

        private fun mapFunction(function: IrSimpleFunction): FunctionSymbol = function.target.let {
            val parent = it.parent

            val containingDeclarationPart = parent.fqNameForIrSerialization.let {
                if (it.isRoot) "" else "$it."
            }
            val name = "kfun:$containingDeclarationPart${it.computeFunctionName()}"
            // A public function is identified across modules by the hash of its binary name - the very name
            // the modules get linked together by, so it is unique and is computed the same way by all of them.
            // This doesn't apply to [FunctionSymbol.External]: several Kotlin declarations (each with
            // its own signature and escape analysis annotations) can be bound to the same native symbol.
            fun computePublicHash() = localHash((if (it.isExported) it.computeSymbolName() else name).toByteArray())

            // External functions and built-in operators don't belong to any of the compiled modules (built-in operators aren't
            // even declared in any klib) - they are always mapped locally to [FunctionSymbol.External], which is deduplicated by hash.
            // Same goes for link-once declarations (see [Type.Public.isLinkOnce]), except they are mapped as ordinary declared
            // functions - just in every module referencing them.
            val isLinkOnce = it.moduleFragment.kotlinLibrary.let { library -> library == null || library.isCInteropLibrary() }
            if (!it.isExternal && !it.isBuiltInOperator && !isLinkOnce
                    && !llvmModuleSpecification.containsDeclaration(it)) {
                val hash = computePublicHash()
                return externalPublicFunctions[hash]
                        ?.also { symbol -> checkNoHashCollision("functions", hash, symbol.name, name) }
                        ?: error("Function ${function.render()} is not found")
            }

            functionMap[it]?.let { return it }

            // Same as for link-once classes (see [mapClassReferenceType]): reuse the copy published by a cached library, if any.
            if (isLinkOnce && it.isExported && !it.isExternal && !it.isBuiltInOperator) {
                val hash = computePublicHash()
                externalPublicFunctions[hash]?.takeIf { symbol -> symbol.isLinkOnce }?.let { symbol ->
                    checkNoHashCollision("functions", hash, symbol.name, name)
                    symbol.irDeclaration = it
                    functionMap[it] = symbol
                    return symbol
                }
            }

            val returnsUnit = it.returnType.isUnit()
            val returnsNothing = it.returnType.isNothing()
            var attributes = 0
            if (returnsUnit)
                attributes = attributes or FunctionAttributes.RETURNS_UNIT
            if (returnsNothing)
                attributes = attributes or FunctionAttributes.RETURNS_NOTHING
            if (it.hasAnnotation(RuntimeNames.exportForCppRuntime)
                    || it.hasAnnotation(RuntimeNames.exportedBridge)
                    || it.hasAnnotation(RuntimeNames.bindReverseBridgeToMethod)
                    || it.getExternalObjCMethodInfo() != null // TODO-DCE-OBJC-INIT
                    || it.hasAnnotation(RuntimeNames.objCMethodImp)) {
                attributes = attributes or FunctionAttributes.EXPLICITLY_EXPORTED
            }
            if (function.isEagerStaticInitializer) {
                attributes = attributes or FunctionAttributes.IS_EAGER_STATIC_INITIALIZER
            }
            if (it.bridgeTarget != null) {
                attributes = attributes or FunctionAttributes.IS_BRIDGE
            }
            val symbol = when {
                it.isExternal || it.isBuiltInOperator -> FunctionSymbol.External(
                        localHash(name.toByteArray()),
                        attributes,
                        it,
                        takeName { name },
                        it.isExported,
                        it.escapes,
                        it.pointsTo
                ).also { symbol -> checkExternalFunction(symbol.hash, symbol.name) }

                else -> {
                    val bridgeTarget = it.bridgeTarget
                    val isSpecialBridge = bridgeTarget.let {
                        it != null && it.getDefaultValueForOverriddenBuiltinFunction() != null
                    }
                    val bridgeTargetSymbol = if (isSpecialBridge || bridgeTarget == null) null else mapFunction(bridgeTarget)
                    val functionSymbol = if (it.isExported)
                        FunctionSymbol.Public(computePublicHash(), module, attributes, it, bridgeTargetSymbol, takeName { name }, isLinkOnce)
                    else
                        FunctionSymbol.Private(privateFunIndex++, module, attributes, it, bridgeTargetSymbol, takeName { name })
                    functionSymbol
                }
            }
            functionMap[it] = symbol
            functionsList += symbol

            symbol.parameters = function.parameters.map { it.type }
                    .map { FunctionParameter(mapType(it)) }
                    .toTypedArray()
            symbol.returnParameter = FunctionParameter(mapType(function.returnType))
            symbol.declaringType = (it.parent as? IrClass)?.let { irClass -> mapClassReferenceType(irClass) }

            return symbol
        }

        private fun mapPropertyInitializer(irField: IrField): FunctionSymbol {
            functionMap[irField]?.let { return it }

            assert(irField.isStatic) { "All local properties initializers should've been lowered" }
            val attributes = FunctionAttributes.IS_STATIC_FIELD_INITIALIZER or FunctionAttributes.RETURNS_UNIT
            val symbol = FunctionSymbol.Private(privateFunIndex++, module, attributes, irField, null, takeName { "${irField.computeSymbolName()}_init" })

            functionMap[irField] = symbol

            symbol.parameters = emptyArray()
            symbol.returnParameter = FunctionParameter(mapType(context.irBuiltIns.unitType))
            return symbol
        }
    }
}
