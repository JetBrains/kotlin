/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.serialization.kotlinLibrary
import org.jetbrains.kotlin.backend.konan.NativeBackendContext
import org.jetbrains.kotlin.backend.konan.PrimitiveBinaryType
import org.jetbrains.kotlin.backend.konan.ir.annotations.Escapes
import org.jetbrains.kotlin.backend.konan.ir.annotations.PointsTo
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.backend.konan.serialization.ByteArrayStream
import kotlin.enums.enumEntries

internal object DFGSerializer {
    class TypeBase(val isFinal: Boolean, val isAbstract: Boolean, val hasFinalizer: Boolean, val name: String?) {
        constructor(data: ByteArrayStream) : this(data.readBoolean(), data.readBoolean(), data.readBoolean(), data.readNullableString())

        fun write(result: ByteArrayStream) {
            result.writeBoolean(isFinal)
            result.writeBoolean(isAbstract)
            result.writeBoolean(hasFinalizer)
            result.writeNullableString(name)
        }
    }

    class ExternalType(val hash: Long, val base: TypeBase) {
        constructor(data: ByteArrayStream) : this(data.readLong(), TypeBase(data))

        fun write(result: ByteArrayStream) {
            result.writeLong(hash)
            base.write(result)
        }
    }

    // TODO: Without GHA, this is just the interface FqName's hash. Is 32 bits safe?
    class ItableSlot(val interfaceId: Int, val vtable: IntArray) {
        constructor(data: ByteArrayStream) : this(data.readInt(), data.readIntArray())

        fun write(result: ByteArrayStream) {
            result.writeInt(interfaceId)
            result.writeIntArray(vtable)
        }
    }

    class DeclaredType(val base: TypeBase, val superTypes: IntArray,
                       val vtable: IntArray, val itable: Array<ItableSlot>) {
        constructor(data: ByteArrayStream) : this(TypeBase(data), data.readIntArray(),
                data.readIntArray(), data.readArray { ItableSlot(this) })

        fun write(result: ByteArrayStream) {
            base.write(result)
            result.writeIntArray(superTypes)
            result.writeIntArray(vtable)
            result.writeArray(itable) { it.write(this) }
        }
    }

    class PublicType(val hash: Long, val isLinkOnce: Boolean, val intestines: DeclaredType) {
        constructor(data: ByteArrayStream) : this(data.readLong(), data.readBoolean(), DeclaredType(data))

        fun write(result: ByteArrayStream) {
            result.writeLong(hash)
            result.writeBoolean(isLinkOnce)
            intestines.write(result)
        }
    }

    class PrivateType(val intestines: DeclaredType) {
        constructor(data: ByteArrayStream) : this(DeclaredType(data))

        fun write(result: ByteArrayStream) {
            intestines.write(result)
        }
    }

    class Type(
            val primitiveBinaryType: PrimitiveBinaryType?,
            val external: ExternalType?,
            val public: PublicType?,
            val private: PrivateType?,
            val virtual: Boolean
    ) {
        fun write(result: ByteArrayStream) {
            val primitivesCount = PrimitiveBinaryType.entries.size
            result.writeByte(
                    when {
                        primitiveBinaryType != null -> primitiveBinaryType.ordinal
                        external != null -> 1 + primitivesCount
                        public != null -> 2 + primitivesCount
                        private != null -> 3 + primitivesCount
                        else -> 0 + primitivesCount
                    }.toByte()
            )
            external?.write(result)
            public?.write(result)
            private?.write(result)
        }

        companion object {
            fun primitive(primitiveBinaryType: PrimitiveBinaryType) =
                    Type(primitiveBinaryType, null, null, null, false)

            fun external(hash: Long, base: TypeBase) =
                    Type(null, ExternalType(hash, base), null, null, false)

            fun public(hash: Long, isLinkOnce: Boolean, intestines: DeclaredType) =
                    Type(null, null, PublicType(hash, isLinkOnce, intestines), null, false)

            fun private(intestines: DeclaredType) =
                    Type(null, null, null, PrivateType(intestines), false)

            fun virtual() = Type(null, null, null, null, true)

            fun read(data: ByteArrayStream): Type {
                val primitivesCount = PrimitiveBinaryType.entries.size
                val tag = data.readByte().toInt()
                return when {
                    tag < primitivesCount ->
                        Type(PrimitiveBinaryType.entries[tag], null, null, null, false)
                    tag == primitivesCount + 1 ->
                        Type(null, ExternalType(data), null, null, false)
                    tag == primitivesCount + 2 ->
                        Type(null, null, PublicType(data), null, false)
                    tag == primitivesCount + 3 ->
                        Type(null, null, null, PrivateType(data), false)
                    else -> Type(null, null, null, null, true)
                }
            }
        }
    }

    class FunctionParameter(val type: Int) {
        constructor(data: ByteArrayStream) : this(data.readInt())

        fun write(result: ByteArrayStream) {
            result.writeInt(type)
        }
    }

    class FunctionSymbolBase(val parameters: Array<FunctionParameter>, val returnParameter: FunctionParameter, val attributes: Int) {
        constructor(data: ByteArrayStream) : this(
                data.readArray { FunctionParameter(data) }, FunctionParameter(data), data.readInt()
        )

        fun write(result: ByteArrayStream) {
            result.writeArray(parameters) { it.write(this) }
            returnParameter.write(result)
            result.writeInt(attributes)
        }
    }

    class ExternalFunctionSymbol(val hash: Long, val name: String?, val isExported: Boolean, val isNative: Boolean, val escapes: Int?, val pointsTo: IntArray?) {
        constructor(data: ByteArrayStream) : this(
                data.readLong(),
                data.readNullableString(),
                data.readBoolean(),
                data.readBoolean(),
                data.readNullableInt(),
                data.readNullable { readIntArray() }
        )

        fun write(result: ByteArrayStream) {
            result.writeLong(hash)
            result.writeNullableString(name)
            result.writeBoolean(isExported)
            result.writeBoolean(isNative)
            result.writeNullableInt(escapes)
            result.writeNullable(pointsTo) { writeIntArray(it) }
        }
    }

    class PublicFunctionSymbol(val hash: Long, val isLinkOnce: Boolean, val bridgeTarget: Int?, val name: String?) {
        constructor(data: ByteArrayStream) : this(data.readLong(), data.readBoolean(),
                data.readNullableInt(), data.readNullableString())

        fun write(result: ByteArrayStream) {
            result.writeLong(hash)
            result.writeBoolean(isLinkOnce)
            result.writeNullableInt(bridgeTarget)
            result.writeNullableString(name)
        }
    }

    class PrivateFunctionSymbol(val bridgeTarget: Int?, val name: String?) {
        constructor(data: ByteArrayStream) : this(data.readNullableInt(), data.readNullableString())

        fun write(result: ByteArrayStream) {
            result.writeNullableInt(bridgeTarget)
            result.writeNullableString(name)
        }
    }

    class FunctionSymbol(
            val base: FunctionSymbolBase,
            val external: ExternalFunctionSymbol?,
            val public: PublicFunctionSymbol?,
            val private: PrivateFunctionSymbol?,
    ) {
        fun write(result: ByteArrayStream) {
            base.write(result)
            result.writeByte(
                    when {
                        external != null -> 1
                        public != null -> 2
                        private != null -> 3
                        else -> 0
                    }.toByte()
            )
            external?.write(result)
            public?.write(result)
            private?.write(result)
        }

        companion object {
            fun external(base: FunctionSymbolBase, hash: Long, name: String?, isExported: Boolean, isNative: Boolean, escapes: Int?, pointsTo: IntArray?) =
                    FunctionSymbol(base, ExternalFunctionSymbol(hash, name, isExported, isNative, escapes, pointsTo), null, null)

            fun public(base: FunctionSymbolBase, hash: Long, isLinkOnce: Boolean, bridgeTarget: Int?, name: String?) =
                    FunctionSymbol(base, null, PublicFunctionSymbol(hash, isLinkOnce, bridgeTarget, name), null)

            fun private(base: FunctionSymbolBase, bridgeTarget: Int?, name: String?) =
                    FunctionSymbol(base, null, null, PrivateFunctionSymbol(bridgeTarget, name))

            fun read(data: ByteArrayStream): FunctionSymbol {
                val base = FunctionSymbolBase(data)
                val tag = data.readByte().toInt()
                return when (tag) {
                    1 -> FunctionSymbol(base, ExternalFunctionSymbol(data), null, null)
                    2 -> FunctionSymbol(base, null, PublicFunctionSymbol(data), null)
                    3 -> FunctionSymbol(base, null, null, PrivateFunctionSymbol(data))
                    else -> FunctionSymbol(base, null, null, null)
                }
            }
        }
    }

    class SymbolTable(val types: Array<Type>, val functionSymbols: Array<FunctionSymbol>) {
        constructor(data: ByteArrayStream) : this(
                data.readArray { Type.read(this) }, data.readArray { FunctionSymbol.read(this) }
        )

        fun write(result: ByteArrayStream) {
            result.writeArray(types) { it.write(this) }
            result.writeArray(functionSymbols) { it.write(this) }
        }
    }

    class Field(val type: Int, val hash: Long, val name: String?) {
        constructor(data: ByteArrayStream) : this(
                data.readInt(), data.readLong(), data.readNullable { readString() }
        )

        fun write(result: ByteArrayStream) {
            result.writeInt(type)
            result.writeLong(hash)
            result.writeNullable(name) { writeString(it) }
        }
    }

    class Edge(val node: Int, val castToType: Int?) {
        constructor(data: ByteArrayStream) : this(data.readInt(), data.readNullableInt())

        fun write(result: ByteArrayStream) {
            result.writeInt(node)
            result.writeNullableInt(castToType)
        }
    }

    class Parameter(val index: Int) {
        constructor(data: ByteArrayStream) : this(data.readInt())

        fun write(result: ByteArrayStream) {
            result.writeInt(index)
        }
    }

    class Const(val type: Int) {
        constructor(data: ByteArrayStream) : this(data.readInt())

        fun write(result: ByteArrayStream) {
            result.writeInt(type)
        }
    }

    class Call(val callee: Int, val arguments: Array<Edge>, val returnType: Int) {
        constructor(data: ByteArrayStream) : this(
                data.readInt(), data.readArray { Edge(this) }, data.readInt()
        )

        fun write(result: ByteArrayStream) {
            result.writeInt(callee)
            result.writeArray(arguments) { it.write(this) }
            result.writeInt(returnType)
        }
    }

    class StaticCall(val call: Call) {
        constructor(data: ByteArrayStream) : this(Call(data))

        fun write(result: ByteArrayStream) {
            call.write(result)
        }
    }

    class VirtualCall(val call: Call, val receiverType: Int) {
        constructor(data: ByteArrayStream) : this(Call(data), data.readInt())

        fun write(result: ByteArrayStream) {
            call.write(result)
            result.writeInt(receiverType)
        }
    }

    class VtableCall(val virtualCall: VirtualCall, val calleeVtableIndex: Int) {
        constructor(data: ByteArrayStream) : this(VirtualCall(data), data.readInt())

        fun write(result: ByteArrayStream) {
            virtualCall.write(result)
            result.writeInt(calleeVtableIndex)
        }
    }

    class ItableCall(val virtualCall: VirtualCall, val interfaceId: Int, val calleeVtableIndex: Int) {
        constructor(data: ByteArrayStream) : this(VirtualCall(data), data.readInt(), data.readInt())

        fun write(result: ByteArrayStream) {
            virtualCall.write(result)
            result.writeInt(interfaceId)
            result.writeInt(calleeVtableIndex)
        }
    }

    class Singleton(val type: Int, val constructor: Int?, val arguments: Array<Edge>?) {
        constructor(data: ByteArrayStream) : this(
                data.readInt(), data.readNullableInt(), data.readNullable { readArray { Edge(this) } }
        )

        fun write(result: ByteArrayStream) {
            result.writeInt(type)
            result.writeNullableInt(constructor)
            result.writeNullable(arguments) { args -> writeArray(args) { it.write(this) } }
        }
    }

    class AllocInstance(val type: Int) {
        constructor(data: ByteArrayStream) : this(data.readInt())

        fun write(result: ByteArrayStream) {
            result.writeInt(type)
        }
    }

    class AllocArray(val type: Int, val size: Edge) {
        constructor(data: ByteArrayStream) : this(data.readInt(), Edge(data))

        fun write(result: ByteArrayStream) {
            result.writeInt(type)
            size.write(result)
        }
    }

    class FunctionReference(val symbol: Int, val type: Int, val returnType: Int) {
        constructor(data: ByteArrayStream) : this(data.readInt(), data.readInt(), data.readInt())

        fun write(result: ByteArrayStream) {
            result.writeInt(symbol)
            result.writeInt(type)
            result.writeInt(returnType)
        }
    }

    class FieldRead(val receiver: Edge?, val field: Field, val type: Int) {
        constructor(data: ByteArrayStream) : this(data.readNullable { Edge(this) }, Field(data), data.readInt())

        fun write(result: ByteArrayStream) {
            result.writeNullable(receiver) { it.write(this) }
            field.write(result)
            result.writeInt(type)
        }
    }

    class FieldWrite(val receiver: Edge?, val field: Field, val value: Edge) {
        constructor(data: ByteArrayStream) : this(data.readNullable { Edge(this) }, Field(data), Edge(data))

        fun write(result: ByteArrayStream) {
            result.writeNullable(receiver) { it.write(this) }
            field.write(result)
            value.write(result)
        }
    }

    class ArrayRead(val callee: Int, val array: Edge, val index: Edge, val type: Int) {
        constructor(data: ByteArrayStream) : this(data.readInt(), Edge(data), Edge(data), data.readInt())

        fun write(result: ByteArrayStream) {
            result.writeInt(callee)
            array.write(result)
            index.write(result)
            result.writeInt(type)
        }
    }

    class ArrayWrite(val callee: Int, val array: Edge, val index: Edge, val value: Edge, val type: Int) {
        constructor(data: ByteArrayStream) : this(
                data.readInt(), Edge(data), Edge(data), Edge(data), data.readInt()
        )

        fun write(result: ByteArrayStream) {
            result.writeInt(callee)
            array.write(result)
            index.write(result)
            value.write(result)
            result.writeInt(type)
        }
    }

    class SaveCoroutineState(val liveVariables: IntArray) {
        constructor(data: ByteArrayStream) : this(data.readIntArray())

        fun write(result: ByteArrayStream) {
            result.writeIntArray(liveVariables)
        }
    }

    class Variable(val values: Array<Edge>, val type: Int, val kind: Byte) {
        constructor(data: ByteArrayStream) : this(data.readArray { Edge(this) }, data.readInt(), data.readByte())

        fun write(result: ByteArrayStream) {
            result.writeArray(values) { it.write(this) }
            result.writeInt(type)
            result.writeByte(kind)
        }
    }

    class Scope(val depth: Int, val nodes: IntArray) {
        constructor(data: ByteArrayStream) : this(data.readInt(), data.readIntArray())

        fun write(result: ByteArrayStream) {
            result.writeInt(depth)
            result.writeIntArray(nodes)
        }
    }

    enum class NodeType {
        UNKNOWN,
        PARAMETER,
        CONST,
        NULL,
        STATIC_CALL,
        VTABLE_CALL,
        ITABLE_CALL,
        SINGLETON,
        ALLOC_INSTANCE,
        ALLOC_ARRAY,
        FUNCTION_REFERENCE,
        FIELD_READ,
        FIELD_WRITE,
        ARRAY_READ,
        ARRAY_WRITE,
        VARIABLE,
        SAVE_COROUTINE_STATE,
        SCOPE,
    }

    class Node {
        var parameter: Parameter? = null
        var const: Const? = null
        var nil: Boolean = false
        var staticCall: StaticCall? = null
        var vtableCall: VtableCall? = null
        var itableCall: ItableCall? = null
        var singleton: Singleton? = null
        var allocInstance: AllocInstance? = null
        var allocArray: AllocArray? = null
        var functionReference: FunctionReference? = null
        var fieldRead: FieldRead? = null
        var fieldWrite: FieldWrite? = null
        var arrayRead: ArrayRead? = null
        var arrayWrite: ArrayWrite? = null
        var variable: Variable? = null
        var saveCoroutineState: SaveCoroutineState? = null
        var scope: Scope? = null

        val type
            get() = when {
                parameter != null -> NodeType.PARAMETER
                const != null -> NodeType.CONST
                staticCall != null -> NodeType.STATIC_CALL
                vtableCall != null -> NodeType.VTABLE_CALL
                itableCall != null -> NodeType.ITABLE_CALL
                singleton != null -> NodeType.SINGLETON
                allocInstance != null -> NodeType.ALLOC_INSTANCE
                allocArray != null -> NodeType.ALLOC_ARRAY
                functionReference != null -> NodeType.FUNCTION_REFERENCE
                fieldRead != null -> NodeType.FIELD_READ
                fieldWrite != null -> NodeType.FIELD_WRITE
                arrayRead != null -> NodeType.ARRAY_READ
                arrayWrite != null -> NodeType.ARRAY_WRITE
                variable != null -> NodeType.VARIABLE
                saveCoroutineState != null -> NodeType.SAVE_COROUTINE_STATE
                scope != null -> NodeType.SCOPE
                nil -> NodeType.NULL
                else -> NodeType.UNKNOWN
            }

        fun write(result: ByteArrayStream) {
            result.writeByte(type.ordinal.toByte())
            parameter?.write(result)
            const?.write(result)
            staticCall?.write(result)
            vtableCall?.write(result)
            itableCall?.write(result)
            singleton?.write(result)
            allocInstance?.write(result)
            allocArray?.write(result)
            functionReference?.write(result)
            fieldRead?.write(result)
            fieldWrite?.write(result)
            arrayRead?.write(result)
            arrayWrite?.write(result)
            variable?.write(result)
            saveCoroutineState?.write(result)
            scope?.write(result)
        }

        companion object {
            fun parameter(index: Int) =
                    Node().also { it.parameter = Parameter(index) }

            fun const(type: Int) =
                    Node().also { it.const = Const(type) }

            fun nil() = Node().also { it.nil = true }

            fun staticCall(call: Call) =
                    Node().also { it.staticCall = StaticCall(call) }

            fun vtableCall(virtualCall: VirtualCall, calleeVtableIndex: Int) =
                    Node().also { it.vtableCall = VtableCall(virtualCall, calleeVtableIndex) }

            fun itableCall(virtualCall: VirtualCall, interfaceId: Int, calleeVtableIndex: Int) =
                    Node().also { it.itableCall = ItableCall(virtualCall, interfaceId, calleeVtableIndex) }

            fun singleton(type: Int, constructor: Int?, arguments: Array<Edge>?) =
                    Node().also { it.singleton = Singleton(type, constructor, arguments) }

            fun allocInst(type: Int) =
                    Node().also { it.allocInstance = AllocInstance(type) }

            fun allocArray(type: Int, size: Edge) =
                    Node().also { it.allocArray = AllocArray(type, size) }

            fun functionReference(symbol: Int, type: Int, returnType: Int) =
                    Node().also { it.functionReference = FunctionReference(symbol, type, returnType) }

            fun fieldRead(receiver: Edge?, field: Field, type: Int) =
                    Node().also { it.fieldRead = FieldRead(receiver, field, type) }

            fun fieldWrite(receiver: Edge?, field: Field, value: Edge) =
                    Node().also { it.fieldWrite = FieldWrite(receiver, field, value) }

            fun arrayRead(callee: Int, array: Edge, index: Edge, type: Int) =
                    Node().also { it.arrayRead = ArrayRead(callee, array, index, type) }

            fun arrayWrite(callee: Int, array: Edge, index: Edge, value: Edge, type: Int) =
                    Node().also { it.arrayWrite = ArrayWrite(callee, array, index, value, type) }

            fun variable(values: Array<Edge>, type: Int, kind: DataFlowIR.VariableKind) =
                    Node().also { it.variable = Variable(values, type, kind.ordinal.toByte()) }

            fun saveCoroutineState(variables: IntArray) =
                    Node().also { it.saveCoroutineState = SaveCoroutineState(variables) }

            fun scope(depth: Int, nodes: IntArray) =
                    Node().also { it.scope = Scope(depth, nodes) }

            fun read(data: ByteArrayStream): Node {
                val nodeTypes = enumEntries<NodeType>()
                val tag = data.readByte().toInt()
                if (tag < 0 || tag >= nodeTypes.size)
                    error("Corrupt data: tag = $tag")
                val type = nodeTypes[tag]
                val result = Node()
                when (type) {
                    NodeType.PARAMETER -> result.parameter = Parameter(data)
                    NodeType.CONST -> result.const = Const(data)
                    NodeType.NULL -> result.nil = true
                    NodeType.STATIC_CALL -> result.staticCall = StaticCall(data)
                    NodeType.VTABLE_CALL -> result.vtableCall = VtableCall(data)
                    NodeType.ITABLE_CALL -> result.itableCall = ItableCall(data)
                    NodeType.SINGLETON -> result.singleton = Singleton(data)
                    NodeType.ALLOC_INSTANCE -> result.allocInstance = AllocInstance(data)
                    NodeType.ALLOC_ARRAY -> result.allocArray = AllocArray(data)
                    NodeType.FUNCTION_REFERENCE -> result.functionReference = FunctionReference(data)
                    NodeType.FIELD_READ -> result.fieldRead = FieldRead(data)
                    NodeType.FIELD_WRITE -> result.fieldWrite = FieldWrite(data)
                    NodeType.ARRAY_READ -> result.arrayRead = ArrayRead(data)
                    NodeType.ARRAY_WRITE -> result.arrayWrite = ArrayWrite(data)
                    NodeType.VARIABLE -> result.variable = Variable(data)
                    NodeType.SAVE_COROUTINE_STATE -> result.saveCoroutineState = SaveCoroutineState(data)
                    NodeType.SCOPE -> result.scope = Scope(data)
                    NodeType.UNKNOWN -> error("Corrupt data: tag = $tag")
                }
                return result
            }
        }
    }

    class FunctionBody(val nodes: Array<Node>, val returns: Int, val throws: Int) {
        constructor(data: ByteArrayStream) : this(
                data.readArray { Node.read(this) }, data.readInt(), data.readInt()
        )

        fun write(result: ByteArrayStream) {
            result.writeArray(nodes) { it.write(this) }
            result.writeInt(returns)
            result.writeInt(throws)
        }
    }

    class Function(val symbol: Int, val body: FunctionBody) {
        constructor(data: ByteArrayStream) : this(data.readInt(), FunctionBody(data))

        fun write(result: ByteArrayStream) {
            result.writeInt(symbol)
            body.write(result)
        }
    }

    class Module(val symbolTable: SymbolTable, val functions: Array<Function>) {
        constructor(data: ByteArrayStream) : this(SymbolTable(data), data.readArray { Function(this) })

        fun write(result: ByteArrayStream) {
            symbolTable.write(result)
            result.writeArray(functions) { it.write(this) }
        }
    }

    private val DEBUG = 0

    private inline fun DEBUG_OUTPUT(severity: Int, block: () -> Unit) {
        if (DEBUG > severity) block()
    }

    fun serialize(moduleDFG: ModuleDFG): ByteArray {
        val symbolTable = moduleDFG.symbolTable
        val typeMap = mutableMapOf<DataFlowIR.Type, Int>()
        val functionSymbolMap = mutableMapOf<DataFlowIR.FunctionSymbol, Int>()
        val publicTypesMap = mutableMapOf<Long, DataFlowIR.Type.Public>()
        val publicFunctionsMap = mutableMapOf<Long, DataFlowIR.FunctionSymbol.Public>()

        // The serialized symbol tables are computed as a closure of everything referenced from the module's DFG, which is not limited
        // to the module's own declarations: nodes can reference types and functions from other modules (resolved by hash during DFG
        // building). Those are serialized as just a hash and get resolved to the defining module's symbols at deserialization.
        fun DataFlowIR.FunctionSymbol.isForeignRef() = this is DataFlowIR.FunctionSymbol.Declared && module !== symbolTable.module

        val registrar = object {
            fun mapType(type: DataFlowIR.Type): Int {
                typeMap[type]?.let { return it }

                val id = typeMap.size
                typeMap[type] = id
                // Only the types declared in this module are serialized along with their intestines
                // (super types, vtable, itable), so close over what the intestines reference.
                // Note: the only locally created types with no IR are the primitives, which have no intestines.
                if (type.module === symbolTable.module && type.irClass != null) {
                    type.superTypes.forEach { mapType(it) }
                    type.vtable.forEach { mapFunctionSymbol(it) }
                    type.itable.values.forEach { vtable -> vtable.forEach { mapFunctionSymbol(it) } }
                }

                return id
            }

            fun mapFunctionSymbol(symbol: DataFlowIR.FunctionSymbol): Int {
                functionSymbolMap[symbol]?.let { return it }

                val id = functionSymbolMap.size
                functionSymbolMap[symbol] = id

                // A reference to another module's function is serialized as just a hash - its signature, bridge target and declaring
                // type come from the defining module. Moreover, they must not be closed over: they can reference that module's private
                // types and functions, which can't be referenced from here and must not be republished as this module's own.
                if (symbol.isForeignRef()) return id

                symbol.parameters.forEach { mapType(it.type) }
                mapType(symbol.returnParameter.type)
                (symbol as? DataFlowIR.FunctionSymbol.Declared)?.bridgeTarget?.let { mapFunctionSymbol(it) }

                return id
            }
        }

        symbolTable.primitiveTypes.forEach { registrar.mapType(it) }
        symbolTable.classMap.values.forEach { registrar.mapType(it) }
        registrar.mapType(DataFlowIR.Type.Virtual)
        symbolTable.functionMap.values.forEach { registrar.mapFunctionSymbol(it) }
        // Note: The DFG holds the functions of all the modules (the ones deserialized from caches included).
        // Only the bodies of the module being compiled are serialized.
        val functions = moduleDFG.functions.values
                .filter { (it.symbol as? DataFlowIR.FunctionSymbol.Declared)?.module === symbolTable.module }
                .map { function ->
                    val body = function.body
                    val nodeMap = mutableMapOf<DataFlowIR.Node, Int>()
                    body.allScopes.forEach { scope -> val _ = nodeMap.getOrPut(scope) { nodeMap.size } }
                    body.forEachNonScopeNode { node -> val _ = nodeMap.getOrPut(node) { nodeMap.size } }
                    val nodes = nodeMap.entries
                            .sortedBy { it.value }
                            .map { entry ->
                                val node = entry.key

                                fun buildEdge(edge: DataFlowIR.Edge) =
                                        Edge(nodeMap[edge.node]!!, edge.castToType?.let { registrar.mapType(it) })

                                fun buildCall(call: DataFlowIR.Node.Call) =
                                        Call(
                                                registrar.mapFunctionSymbol(call.callee),
                                                call.arguments.map { buildEdge(it) }.toTypedArray(),
                                                registrar.mapType(call.returnType)
                                        )

                                fun buildVirtualCall(virtualCall: DataFlowIR.Node.VirtualCall) =
                                        VirtualCall(buildCall(virtualCall), registrar.mapType(virtualCall.receiverType))

                                fun buildField(field: DataFlowIR.Field) =
                                        Field(registrar.mapType(field.type), field.hash, field.name)

                                when (node) {
                                    is DataFlowIR.Node.Parameter -> Node.parameter(node.index)

                                    is DataFlowIR.Node.Const -> Node.const(registrar.mapType(node.type))

                                    DataFlowIR.Node.Null -> Node.nil()

                                    is DataFlowIR.Node.StaticCall ->
                                        Node.staticCall(buildCall(node))

                                    is DataFlowIR.Node.VtableCall ->
                                        Node.vtableCall(buildVirtualCall(node), node.calleeVtableIndex)

                                    is DataFlowIR.Node.ItableCall ->
                                        Node.itableCall(buildVirtualCall(node), node.interfaceId, node.calleeVtableIndex)

                                    is DataFlowIR.Node.Singleton ->
                                        Node.singleton(
                                                registrar.mapType(node.type),
                                                node.constructor?.let { registrar.mapFunctionSymbol(it) },
                                                node.arguments?.let { arguments -> Array(arguments.size) { buildEdge(arguments[it]) } }
                                        )

                                    is DataFlowIR.Node.AllocInstance ->
                                        Node.allocInst(registrar.mapType(node.type))

                                    is DataFlowIR.Node.AllocArray ->
                                        Node.allocArray(registrar.mapType(node.type), buildEdge(node.size))

                                    is DataFlowIR.Node.FunctionReference ->
                                        Node.functionReference(registrar.mapFunctionSymbol(node.symbol), registrar.mapType(node.type), registrar.mapType(node.returnType))

                                    is DataFlowIR.Node.FieldRead ->
                                        Node.fieldRead(node.receiver?.let { buildEdge(it) }, buildField(node.field), registrar.mapType(node.type))

                                    is DataFlowIR.Node.FieldWrite ->
                                        Node.fieldWrite(node.receiver?.let { buildEdge(it) }, buildField(node.field), buildEdge(node.value))

                                    is DataFlowIR.Node.ArrayRead ->
                                        Node.arrayRead(registrar.mapFunctionSymbol(node.callee), buildEdge(node.array), buildEdge(node.index), registrar.mapType(node.type))

                                    is DataFlowIR.Node.ArrayWrite ->
                                        Node.arrayWrite(registrar.mapFunctionSymbol(node.callee), buildEdge(node.array), buildEdge(node.index), buildEdge(node.value), registrar.mapType(node.type))

                                    is DataFlowIR.Node.Variable ->
                                        Node.variable(node.values.map { buildEdge(it) }.toTypedArray(), registrar.mapType(node.type), node.kind)

                                    is DataFlowIR.Node.SaveCoroutineState ->
                                        Node.saveCoroutineState(IntArray(node.liveVariables.size) { nodeMap[node.liveVariables[it]]!! })

                                    is DataFlowIR.Node.Scope ->
                                        Node.scope(node.depth, node.nodes.map { nodeMap[it]!! }.toIntArray())

                                    else -> error("Unknown node $node")
                                }
                            }
                            .toTypedArray()
                    Function(
                            symbol = registrar.mapFunctionSymbol(function.symbol),
                            body = FunctionBody(nodes, nodeMap[body.returns]!!, nodeMap[body.throws]!!)
                    )
                }
                .toTypedArray()
        // Now that every type and function symbol referenced by the bodies has been registered,
        // the maps are complete and can be serialized into the symbol table.
        val types = typeMap.entries
                .sortedBy { it.value }
                .map { entry ->
                    fun buildTypeBase(type: DataFlowIR.Type) =
                            TypeBase(type.isFinal, type.isAbstract, type.hasFinalizer, type.name)

                    fun buildTypeIntestines(type: DataFlowIR.Type) =
                            DeclaredType(
                                    buildTypeBase(type),
                                    IntArray(type.superTypes.size) { typeMap[type.superTypes[it]]!! },
                                    IntArray(type.vtable.size) { functionSymbolMap[type.vtable[it]]!! },
                                    type.itable.map { [interfaceId, vtable] ->
                                        ItableSlot(interfaceId, IntArray(vtable.size) { functionSymbolMap[vtable[it]]!! })
                                    }.toTypedArray()
                            )

                    when (val type = entry.key) {
                        DataFlowIR.Type.Virtual -> Type.virtual()

                        is DataFlowIR.Type.Private -> {
                            // Another module's private type has no cross-module identity to be referenced by.
                            require(type.module === symbolTable.module) { "A private type of another module: ${type.name}" }
                            Type.private(buildTypeIntestines(type))
                        }

                        is DataFlowIR.Type.Public -> when {
                            type.module !== symbolTable.module -> {
                                // A type from another module - serialize only a reference to it.
                                Type.external(type.hash, buildTypeBase(type))
                            }
                            type.irClass == null -> {
                                // The only locally created types with no IR are the primitives.
                                Type.primitive(PrimitiveBinaryType.entries[type.hash.toInt()])
                            }
                            else -> {
                                val prevType = publicTypesMap[type.hash]
                                if (prevType != null)
                                    error("Exported types name clash ${type.hash}: ${prevType.name} and ${type.name}")
                                publicTypesMap[type.hash] = type
                                Type.public(type.hash, type.isLinkOnce, buildTypeIntestines(type))
                            }
                        }

                        else -> error("Unknown type $type")
                    }
                }
                .toTypedArray()
        val functionSymbols = functionSymbolMap.entries
                .sortedBy { it.value }
                .map { entry ->
                    fun buildFunctionParameter(parameter: DataFlowIR.FunctionParameter) =
                            FunctionParameter(typeMap[parameter.type]!!)

                    fun buildFunctionSymbolBase(symbol: DataFlowIR.FunctionSymbol) =
                            if (symbol.isForeignRef()) {
                                // The signature of a reference to another module's function isn't serialized
                                // (see the registrar) - it gets resolved to the defining module's symbol.
                                FunctionSymbolBase(
                                        emptyArray(),
                                        FunctionParameter(typeMap[DataFlowIR.Type.Virtual]!!),
                                        symbol.attributes,
                                )
                            } else {
                                FunctionSymbolBase(
                                        symbol.parameters.map { buildFunctionParameter(it) }.toTypedArray(),
                                        buildFunctionParameter(symbol.returnParameter),
                                        symbol.attributes,
                                )
                            }

                    val symbol = entry.key
                    val bridgeTarget = (symbol as? DataFlowIR.FunctionSymbol.Declared)
                            ?.takeIf { !it.isForeignRef() }?.bridgeTarget?.let { functionSymbolMap[it]!! }
                    when (symbol) {
                        is DataFlowIR.FunctionSymbol.Private -> {
                            // Another module's private function has no cross-module identity to be referenced by.
                            require(symbol.module === symbolTable.module) { "A private function of another module: ${symbol.name}" }
                            FunctionSymbol.private(buildFunctionSymbolBase(symbol), bridgeTarget, symbol.name)
                        }
                        is DataFlowIR.FunctionSymbol.External -> {
                            FunctionSymbol.external(
                                    buildFunctionSymbolBase(symbol), symbol.hash, symbol.name,
                                    symbol.isExported, isNative = true, symbol.escapes?.mask, symbol.pointsTo?.elements,
                            )
                        }
                        is DataFlowIR.FunctionSymbol.Public -> {
                            if (symbol.module !== symbolTable.module) {
                                // A Kotlin function from another module - it is published by its defining module,
                                // so serialize only a reference to it.
                                FunctionSymbol.external(
                                        buildFunctionSymbolBase(symbol), symbol.hash, symbol.name,
                                        isExported = true, isNative = false, escapes = null, pointsTo = null,
                                )
                            } else {
                                // Note: all the public functions of this module are published, even the bodiless ones
                                // (like abstract methods, which can be referenced as callees of virtual calls).
                                val prevSymbol = publicFunctionsMap[symbol.hash]
                                if (prevSymbol != null)
                                    error("Exported functions name clash ${symbol.hash}: ${prevSymbol.name} and ${symbol.name}")
                                publicFunctionsMap[symbol.hash] = symbol
                                FunctionSymbol.public(buildFunctionSymbolBase(symbol), symbol.hash,
                                        symbol.isLinkOnce, bridgeTarget, symbol.name)
                            }
                        }
                        else -> error("Unknown function symbol $symbol")
                    }
                }
                .toTypedArray()
        DEBUG_OUTPUT(0) {
            println("TYPES: ${typeMap.size}, " +
                    "FUNCTIONS: ${functionSymbolMap.size}, " +
                    "PRIVATE FUNCTIONS: ${functionSymbolMap.keys.count { it is DataFlowIR.FunctionSymbol.Private }}, " +
                    "FUNCTION TABLE SIZE: ${symbolTable.privateFunIndex}"
            )
        }
        val module = Module(SymbolTable(types, functionSymbols), functions)
        val writer = ByteArrayStream()
        module.write(writer)
        return writer.toByteArray()
    }

    /**
     * Deserializes the DFGs of all the cached libraries into [symbolTable], publishing the public
     * types into [DataFlowIR.SymbolTable.externalPublicTypes], public functions into
     * [DataFlowIR.SymbolTable.externalPublicFunctions], and returns the deserialized function DFGs.
     */
    fun deserialize(
            context: NativeBackendContext,
            symbolTable: DataFlowIR.SymbolTable,
    ): MutableMap<DataFlowIR.FunctionSymbol, DataFlowIR.Function> {
        val publicTypesMap = symbolTable.externalPublicTypes
        val allTypes = mutableListOf<DataFlowIR.Type>()
        val publicFunctionsMap = symbolTable.externalPublicFunctions
        val functions = mutableMapOf<DataFlowIR.FunctionSymbol, DataFlowIR.Function>()
        // Relies on irModules being in reverse topo-order (see KT-61096): external references in a library's DFG
        // can only be resolved if all its dependencies have already been deserialized and have published their symbols.
        val libraries = context.irModules.map { it.kotlinLibrary!! }
        libraries.forEach { library ->
            // A cinterop library's cache carries no DFG by design - its declarations are mapped
            // locally by every module referencing them (see TopLevelPhases.runCodegen).
            if (library.isCInteropLibrary()) return@forEach
            val cache = context.config.cachedLibraries.getLibraryCache(library)
            val libraryDataFlowGraph = cache?.serializedDataFlowGraph

            // For a Kotlin library a missing DFG means the cache was built by an older compiler
            // or without optimizations - the whole-world analyses cannot soundly proceed without
            // the library's part of the graph.
            if (cache != null && libraryDataFlowGraph == null) {
                error("The cache for '${library.uniqueName}' at ${cache.rootDirectory} carries no serialized data flow graph. " +
                        "It was likely built by an older compiler or without optimizations enabled - delete it to force a rebuild.")
            }

            DEBUG_OUTPUT(0) {
                println("Data flow graph size for lib '${library.uniqueName}': ${libraryDataFlowGraph?.size ?: 0}")
            }

            if (libraryDataFlowGraph != null) {
                val module = DataFlowIR.Module()
                val reader = ByteArrayStream(libraryDataFlowGraph)
                val moduleDataFlowGraph = Module(reader)

                val moduleSymbolTable = moduleDataFlowGraph.symbolTable
                // Link-once declarations (see [DataFlowIR.Type.Public.isLinkOnce]) are compiled into every module
                // referencing them, so several libraries' DFGs can publish a copy of the same declaration.
                // The first published symbol wins; the copies are skipped, along with their intestines.
                // For anything not marked link-once hash clashing is still an error.
                val deduplicatedTypes = mutableSetOf<Int>()
                val deduplicatedFunctions = mutableSetOf<Int>()
                val types = moduleSymbolTable.types.mapIndexed { index, serializedType ->
                    val primitiveBinaryType = serializedType.primitiveBinaryType
                    val external = serializedType.external
                    val public = serializedType.public
                    val private = serializedType.private
                    when {
                        serializedType.virtual -> DataFlowIR.Type.Virtual
                        primitiveBinaryType != null -> symbolTable.primitiveTypes[primitiveBinaryType.ordinal]
                        external != null ->
                            publicTypesMap[external.hash] ?: error("External type ${external.base.name} : ${external.hash} is not found")
                        public != null -> {
                            val prevType = publicTypesMap[public.hash]
                            when {
                                prevType == null ->
                                    DataFlowIR.Type.Public(public.hash, symbolTable.typeIndex++, public.intestines.base.isFinal,
                                            public.intestines.base.isAbstract, public.intestines.base.hasFinalizer, module, null, public.intestines.base.name,
                                            public.isLinkOnce).also {
                                        publicTypesMap[it.hash] = it
                                        allTypes += it
                                        symbolTable.typesList += it
                                    }
                                public.isLinkOnce && prevType.isLinkOnce -> {
                                    deduplicatedTypes += index
                                    prevType
                                }
                                else -> error("Exported types name clash ${public.hash}: ${prevType.name} and ${public.intestines.base.name}")
                            }
                        }
                        else -> {
                            DataFlowIR.Type.Private(symbolTable.typeIndex++, private!!.intestines.base.isFinal,
                                    private.intestines.base.isAbstract, private.intestines.base.hasFinalizer,
                                    module, null, private.intestines.base.name).also {
                                allTypes += it
                                symbolTable.typesList += it
                            }
                        }
                    }
                }

                val functionSymbols = moduleSymbolTable.functionSymbols.mapIndexed { index, serializedSymbol ->
                    val attributes = serializedSymbol.base.attributes
                    val external = serializedSymbol.external
                    val public = serializedSymbol.public
                    val private = serializedSymbol.private
                    when {
                        external != null -> when {
                            external.isNative -> {
                                DataFlowIR.FunctionSymbol.External(
                                        external.hash, attributes, null, external.name,
                                        external.isExported,
                                        external.escapes?.let { Escapes(it) },
                                        external.pointsTo?.let { PointsTo(it) }
                                ).also {
                                    symbolTable.functionsList += it
                                }
                            }
                            else -> {
                                publicFunctionsMap[external.hash] ?: error("External function ${external.name} : ${external.hash} is not found")
                            }
                        }
                        public != null -> {
                            val prevSymbol = publicFunctionsMap[public.hash]
                            when {
                                prevSymbol == null ->
                                    DataFlowIR.FunctionSymbol.Public(public.hash,
                                            module, attributes, null, null, public.name,
                                            public.isLinkOnce).also {
                                        publicFunctionsMap[it.hash] = it
                                        symbolTable.functionsList += it
                                    }
                                public.isLinkOnce && prevSymbol.isLinkOnce -> {
                                    deduplicatedFunctions += index
                                    prevSymbol
                                }
                                else -> error("Exported functions name clash ${public.hash}: ${prevSymbol.name} and ${public.name}")
                            }
                        }
                        else -> {
                            DataFlowIR.FunctionSymbol.Private(symbolTable.privateFunIndex++,
                                    module, attributes, null, null, private!!.name).also {
                                symbolTable.functionsList += it
                            }
                        }
                    }
                }

                DEBUG_OUTPUT(0) {
                    println("Lib: ${library.uniqueName}, types: ${types.size}, functions: ${functionSymbols.size}")
                }

                moduleSymbolTable.types.forEachIndexed { index, type ->
                    if (index in deduplicatedTypes) return@forEachIndexed
                    val intestines = type.public?.intestines ?: type.private?.intestines ?: return@forEachIndexed
                    val deserializedType = types[index]
                    deserializedType.superTypes += intestines.superTypes.map { types[it] }
                    deserializedType.vtable += intestines.vtable.map { functionSymbols[it] }
                    intestines.itable.forEach { itableSlot ->
                        deserializedType.itable[itableSlot.interfaceId] = itableSlot.vtable.map { functionSymbols[it] }
                    }
                }

                moduleSymbolTable.functionSymbols.forEachIndexed { index, symbol ->
                    // A reference to another module's Kotlin function resolves to the symbol published
                    // by its defining module - its parameters and bridge target are already set.
                    // The same goes for a deduplicated copy of a link-once function.
                    if (symbol.external?.isNative == false) return@forEachIndexed
                    if (index in deduplicatedFunctions) return@forEachIndexed
                    val deserializedSymbol = functionSymbols[index]
                    deserializedSymbol.parameters = symbol.base.parameters.map { DataFlowIR.FunctionParameter(types[it.type]) }.toTypedArray()
                    deserializedSymbol.returnParameter = DataFlowIR.FunctionParameter(types[symbol.base.returnParameter.type])
                    deserializedSymbol as? DataFlowIR.FunctionSymbol.Declared ?: return@forEachIndexed
                    val bridgeTarget = if (deserializedSymbol is DataFlowIR.FunctionSymbol.Public)
                        symbol.public!!.bridgeTarget
                    else
                        symbol.private!!.bridgeTarget
                    deserializedSymbol.bridgeTarget = bridgeTarget?.let { functionSymbols[it] }
                }

                fun deserializeEdge(edge: Edge) =
                        DataFlowIR.Edge(edge.castToType?.let { types[it] })

                fun deserializeCall(call: Call) =
                        DataFlowIR.Node.Call(
                                functionSymbols[call.callee],
                                call.arguments.map { deserializeEdge(it) },
                                types[call.returnType],
                                irCallSite = null
                        )

                fun deserializeField(field: Field) = DataFlowIR.Field(types[field.type], field.hash, field.name)

                fun deserializeBody(body: FunctionBody): DataFlowIR.FunctionBody {
                    val nodes = body.nodes.map { node ->
                        when (node.type) {
                            NodeType.PARAMETER -> {
                                DataFlowIR.Node.Parameter(node.parameter!!.index)
                            }
                            NodeType.CONST -> {
                                DataFlowIR.Node.Const(types[node.const!!.type])
                            }
                            NodeType.NULL -> {
                                DataFlowIR.Node.Null
                            }
                            NodeType.STATIC_CALL -> {
                                val staticCall = node.staticCall!!
                                val call = deserializeCall(staticCall.call)
                                DataFlowIR.Node.StaticCall(call.callee, call.arguments, call.returnType, irCallSite = null)
                            }
                            NodeType.VTABLE_CALL -> {
                                val vtableCall = node.vtableCall!!
                                val call = deserializeCall(vtableCall.virtualCall.call)
                                DataFlowIR.Node.VtableCall(
                                        call.callee,
                                        types[vtableCall.virtualCall.receiverType],
                                        vtableCall.calleeVtableIndex,
                                        call.arguments,
                                        types[vtableCall.virtualCall.call.returnType],
                                        irCallSite = null,
                                )
                            }
                            NodeType.ITABLE_CALL -> {
                                val itableCall = node.itableCall!!
                                val call = deserializeCall(itableCall.virtualCall.call)
                                DataFlowIR.Node.ItableCall(
                                        call.callee,
                                        types[itableCall.virtualCall.receiverType],
                                        itableCall.interfaceId,
                                        itableCall.calleeVtableIndex,
                                        call.arguments,
                                        types[itableCall.virtualCall.call.returnType],
                                        irCallSite = null,
                                )
                            }
                            NodeType.SINGLETON -> {
                                val singleton = node.singleton!!
                                DataFlowIR.Node.Singleton(
                                        types[singleton.type],
                                        singleton.constructor?.let { functionSymbols[it] },
                                        singleton.arguments?.map { deserializeEdge(it) },
                                )
                            }
                            NodeType.ALLOC_INSTANCE -> {
                                DataFlowIR.Node.AllocInstance(types[node.allocInstance!!.type], irCallSite = null)
                            }
                            NodeType.ALLOC_ARRAY -> {
                                val allocArray = node.allocArray!!
                                DataFlowIR.Node.AllocArray(types[allocArray.type], deserializeEdge(allocArray.size), irCallSite = null)
                            }
                            NodeType.FUNCTION_REFERENCE -> {
                                val functionReference = node.functionReference!!
                                DataFlowIR.Node.FunctionReference(
                                        functionSymbols[functionReference.symbol],
                                        types[functionReference.type],
                                        types[functionReference.returnType],
                                )
                            }
                            NodeType.FIELD_READ -> {
                                val fieldRead = node.fieldRead!!
                                val receiver = fieldRead.receiver?.let { deserializeEdge(it) }
                                DataFlowIR.Node.FieldRead(
                                        receiver,
                                        deserializeField(fieldRead.field),
                                        types[fieldRead.type],
                                        ir = null,
                                )
                            }
                            NodeType.FIELD_WRITE -> {
                                val fieldWrite = node.fieldWrite!!
                                val receiver = fieldWrite.receiver?.let { deserializeEdge(it) }
                                DataFlowIR.Node.FieldWrite(
                                        receiver,
                                        deserializeField(fieldWrite.field),
                                        deserializeEdge(fieldWrite.value),
                                )
                            }
                            NodeType.ARRAY_READ -> {
                                val arrayRead = node.arrayRead!!
                                DataFlowIR.Node.ArrayRead(
                                        functionSymbols[arrayRead.callee],
                                        deserializeEdge(arrayRead.array),
                                        deserializeEdge(arrayRead.index),
                                        types[arrayRead.type],
                                        irCallSite = null,
                                )
                            }
                            NodeType.ARRAY_WRITE -> {
                                val arrayWrite = node.arrayWrite!!
                                DataFlowIR.Node.ArrayWrite(
                                        functionSymbols[arrayWrite.callee],
                                        deserializeEdge(arrayWrite.array),
                                        deserializeEdge(arrayWrite.index),
                                        deserializeEdge(arrayWrite.value),
                                        types[arrayWrite.type],
                                )
                            }
                            NodeType.VARIABLE -> {
                                val variable = node.variable!!
                                DataFlowIR.Node.Variable(variable.values.map { deserializeEdge(it) },
                                        types[variable.type], enumEntries<DataFlowIR.VariableKind>()[variable.kind.toInt()])
                            }
                            NodeType.SAVE_COROUTINE_STATE -> {
                                DataFlowIR.Node.SaveCoroutineState(emptyList())
                            }
                            NodeType.SCOPE -> {
                                val scope = node.scope!!
                                DataFlowIR.Node.Scope(scope.depth, emptyList())
                            }
                            NodeType.UNKNOWN -> error("Unknown node: $node")
                        }
                    }

                    lateinit var rootScope: DataFlowIR.Node.Scope
                    val allScopes = mutableListOf<DataFlowIR.Node.Scope>()
                    body.nodes.forEachIndexed { index, node ->
                        val deserializedNode = nodes[index]
                        when (node.type) {
                            NodeType.STATIC_CALL ->
                                node.staticCall!!.call.arguments.forEachIndexed { i, arg ->
                                    (deserializedNode as DataFlowIR.Node.StaticCall).arguments[i].node = nodes[arg.node]
                                }
                            NodeType.VTABLE_CALL ->
                                node.vtableCall!!.virtualCall.call.arguments.forEachIndexed { i, arg ->
                                    (deserializedNode as DataFlowIR.Node.VtableCall).arguments[i].node = nodes[arg.node]
                                }
                            NodeType.ITABLE_CALL ->
                                node.itableCall!!.virtualCall.call.arguments.forEachIndexed { i, arg ->
                                    (deserializedNode as DataFlowIR.Node.ItableCall).arguments[i].node = nodes[arg.node]
                                }
                            NodeType.SINGLETON -> (deserializedNode as DataFlowIR.Node.Singleton).arguments?.let { arguments ->
                                node.singleton!!.arguments!!.forEachIndexed { i, arg ->
                                    arguments[i].node = nodes[arg.node]
                                }
                            }
                            NodeType.ALLOC_ARRAY -> {
                                (deserializedNode as DataFlowIR.Node.AllocArray).size.node = nodes[node.allocArray!!.size.node]
                            }
                            NodeType.FIELD_READ ->
                                node.fieldRead!!.receiver?.let {
                                    (deserializedNode as DataFlowIR.Node.FieldRead).receiver!!.node = nodes[it.node]
                                }
                            NodeType.FIELD_WRITE -> {
                                val deserializedFieldWrite = deserializedNode as DataFlowIR.Node.FieldWrite
                                val fieldWrite = node.fieldWrite!!
                                fieldWrite.receiver?.let { deserializedFieldWrite.receiver!!.node = nodes[it.node] }
                                deserializedFieldWrite.value.node = nodes[fieldWrite.value.node]
                            }
                            NodeType.ARRAY_READ -> {
                                val deserializedArrayRead = deserializedNode as DataFlowIR.Node.ArrayRead
                                val arrayRead = node.arrayRead!!
                                deserializedArrayRead.array.node = nodes[arrayRead.array.node]
                                deserializedArrayRead.index.node = nodes[arrayRead.index.node]
                            }
                            NodeType.ARRAY_WRITE -> {
                                val deserializedArrayWrite = deserializedNode as DataFlowIR.Node.ArrayWrite
                                val arrayWrite = node.arrayWrite!!
                                deserializedArrayWrite.array.node = nodes[arrayWrite.array.node]
                                deserializedArrayWrite.index.node = nodes[arrayWrite.index.node]
                                deserializedArrayWrite.value.node = nodes[arrayWrite.value.node]
                            }
                            NodeType.VARIABLE ->
                                node.variable!!.values.forEachIndexed { i, value ->
                                    (deserializedNode as DataFlowIR.Node.Variable).values[i].node = nodes[value.node]
                                }
                            NodeType.SAVE_COROUTINE_STATE ->
                                node.saveCoroutineState!!.liveVariables.mapTo((deserializedNode as DataFlowIR.Node.SaveCoroutineState).liveVariables) {
                                    nodes[it] as DataFlowIR.Node.Variable
                                }
                            NodeType.SCOPE -> {
                                val scope = deserializedNode as DataFlowIR.Node.Scope
                                node.scope!!.nodes.mapTo(scope.nodes) { nodes[it] }
                                if (scope.depth == 0)
                                    rootScope = scope
                                allScopes += scope
                            }
                            else -> {}
                        }
                    }

                    return DataFlowIR.FunctionBody(rootScope, allScopes, nodes[body.returns] as DataFlowIR.Node.Variable, nodes[body.throws] as DataFlowIR.Node.Variable)
                }

                moduleDataFlowGraph.functions.forEach {
                    val symbol = functionSymbols[it.symbol]
                    val function = DataFlowIR.Function(
                            symbol = symbol,
                            body = deserializeBody(it.body)
                    )
                    functions[symbol] = function
                }
            }
        }

        DEBUG_OUTPUT(1) {
            allTypes.forEach {
                println(it)
                println("    SUPER TYPES:")
                it.superTypes.forEach { println("        $it") }
                println("    VTABLE:")
                it.vtable.forEach { println("        $it") }
                println("    ITABLE:")
                it.itable.forEach { t, u -> println("        $t: $u") }
            }

            functions.forEach {
                println(it.key)
            }
        }

        return functions
    }
}
