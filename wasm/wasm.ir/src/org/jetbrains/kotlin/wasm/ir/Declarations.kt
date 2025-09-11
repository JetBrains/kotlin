/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation

const val NO_FUNC_IDX = -1
// TODO decide which table is most used and make it default (0), after making all of them imported
const val EXTERNREF_TABLE = 0
const val FUNCTIONS_TABLE = 1

class WasmModule(
    val recGroups: List<List<WasmTypeDeclaration>> = emptyList(),
    val importsInOrder: List<WasmNamedModuleField> = emptyList(),
    val importedFunctions: List<WasmFunction.Imported> = emptyList(),
    val importedMemories: List<WasmMemory> = emptyList(),
    val importedTables: List<WasmTable> = emptyList(),
    val importedGlobals: List<WasmGlobal> = emptyList(),
    val importedTags: List<WasmTag> = emptyList(),

    val definedFunctions: List<WasmFunction.Defined> = emptyList(),
    val tables: List<WasmTable> = emptyList(),
    val memories: List<WasmMemory> = emptyList(),
    val globals: List<WasmGlobal> = emptyList(),
    val exports: List<WasmExport<*>> = emptyList(),
    val elements: List<WasmElement> = emptyList(),
    val tags: List<WasmTag> = emptyList(),

    val startFunction: WasmFunction? = null,

    val data: List<WasmData> = emptyList(),
    val dataCount: Boolean = true,
)

sealed class WasmNamedModuleField {
    var id: Int? = null
    open val name: String = ""
}

sealed class WasmFunction(
    override val name: String,
    val type: WasmSymbolReadOnly<WasmFunctionType>,
) : WasmNamedModuleField() {
    class Defined(
        name: String,
        type: WasmSymbolReadOnly<WasmFunctionType>,
        val locals: MutableList<WasmLocal> = mutableListOf(),
        val instructions: MutableList<WasmInstr> = mutableListOf(),
        val startLocation: SourceLocation = SourceLocation.IgnoredLocation,
        val endLocation: SourceLocation = SourceLocation.IgnoredLocation,
    ) : WasmFunction(name, type)

    class Imported(
        name: String,
        type: WasmSymbolReadOnly<WasmFunctionType>,
        val importPair: WasmImportDescriptor,
    ) : WasmFunction(name, type)

    override fun toString(): String {
        return name
    }
}

class WasmMemory(
    val limits: WasmLimits,
    val importPair: WasmImportDescriptor? = null,
) : WasmNamedModuleField()

sealed class WasmDataMode {
    class Active(
        val memoryIdx: Int,
        val offset: MutableList<WasmInstr>
    ) : WasmDataMode() {
        constructor(memoryIdx: Int, offset: Int) : this(memoryIdx, mutableListOf<WasmInstr>().also<MutableList<WasmInstr>> {
            WasmExpressionBuilder(it).buildConstI32(offset, SourceLocation.NoLocation("Offset value for WasmDataMode.Active "))
        })
    }

    object Passive : WasmDataMode()
}

class WasmData(
    val mode: WasmDataMode,
    val bytes: ByteArray,
) : WasmNamedModuleField()

class WasmTable(
    var limits: WasmLimits = WasmLimits(1u, null),
    val elementType: WasmType,
    val importPair: WasmImportDescriptor? = null
) : WasmNamedModuleField() {

    sealed class Value {
        class Function(val function: WasmSymbol<WasmFunction>) : Value() {
            constructor(function: WasmFunction) : this(WasmSymbol(function))
        }

        class Expression(val expr: List<WasmInstr>) : Value()
    }

}

class WasmElement(
    val type: WasmType,
    val values: List<WasmTable.Value>,
    val mode: Mode,
) : WasmNamedModuleField() {
    sealed class Mode {
        object Passive : Mode()
        class Active(val table: WasmTable, val offset: List<WasmInstr>) : Mode() {
            constructor(table: WasmTable, offset: Int) : this(table, mutableListOf<WasmInstr>().also<MutableList<WasmInstr>> {
                WasmExpressionBuilder(it).buildConstI32(offset, SourceLocation.NoLocation("Offset value for WasmElement.Mode.Active"))
            })
        }
        object Declarative : Mode()
    }
}

class WasmTag(
    val type: WasmFunctionType,
    val importPair: WasmImportDescriptor? = null
) : WasmNamedModuleField() {
    init {
        assert(type.resultTypes.isEmpty()) { "Must have empty return as per current spec" }
    }
}

class WasmLocal(
    val id: Int,
    val name: String,
    val type: WasmType,
    val isParameter: Boolean
)

sealed class AbstractWasmGlobal(
    override val name: String,
    val type: WasmType,
    val isMutable: Boolean,
    val importPair: WasmImportDescriptor? = null,
    protected var _init: List<WasmInstr>? = null,
) : WasmNamedModuleField()
{
    val init: List<WasmInstr>
        get() = _init ?: error("Init is not materialized for deferred global $this")

    val isDeferred = _init == null
}

open class WasmGlobal protected constructor(
    name: String,
    type: WasmType,
    isMutable: Boolean,
    importPair: WasmImportDescriptor? = null,
    _init: List<WasmInstr>? = null,
) : AbstractWasmGlobal(name, type, isMutable, importPair, _init) {

    constructor(
        name: String,
        type: WasmType,
        isMutable: Boolean,
        init: List<WasmInstr>,
        importPair: WasmImportDescriptor? = null
    ) : this(name, type, isMutable, _init = init, importPair = importPair)
}

// Global with incomplete initializer. It must be "materialized" by replacing each func ref to the corresponding index in the function table
class DeferredWasmGlobal(name: String, type: WasmType, isMutable: Boolean, val initTemplate: List<WasmInstr>) :
    WasmGlobal(name, type, isMutable)
{
    fun materialize(moduleTableMethodsMap: Map<WasmSymbol<WasmFunction>, Int>) {
        fun WasmInstr.isRefNullNoFunc() : Boolean {
            if (operator != WasmOp.REF_NULL) return false
            if (immediates.size != 1) return false
            val immediate = immediates[0]
            if (immediate !is WasmImmediate.HeapType) return false
            return immediate.value == WasmHeapType.Simple.NoFunc
        }

        fun WasmInstr.refFuncValue(): WasmSymbol<WasmFunction> {
            check (operator == WasmOp.REF_FUNC)
            check (immediates.size == 1)
            val immediate = immediates[0]
            check (immediate is WasmImmediate.FuncIdx)
            return immediate.value
        }

        fun getRefFuncTableIndex(refFuncInstr: WasmInstr): Int =
            refFuncInstr.refFuncValue().let { moduleTableMethodsMap[it] ?: error("Module table shall contain method ${it}") }

        _init = buildWasmExpression {
            for (instr in initTemplate) {
                val location = instr.location ?: SourceLocation.NoLocation("DeferredVTableWasmGlobal")
                when (instr.operator) {
                    WasmOp.REF_FUNC -> buildInstr(WasmOp.I32_CONST, location, WasmImmediate.ConstI32(getRefFuncTableIndex(instr)))
                    WasmOp.REF_NULL if instr.isRefNullNoFunc() -> buildInstr(WasmOp.I32_CONST, location, WasmImmediate.ConstI32(NO_FUNC_IDX))
                    else -> buildInstr(instr.operator, location, *instr.immediates.toTypedArray())
                }
            }
        }
    }
}

sealed class WasmExport<T : WasmNamedModuleField>(
    val name: String,
    val field: T,
    val kind: Byte,
    val keyword: String
) {
    class Function(name: String, field: WasmFunction) : WasmExport<WasmFunction>(name, field, 0x0, "func")
    class Table(name: String, field: WasmTable) : WasmExport<WasmTable>(name, field, 0x1, "table")
    class Memory(name: String, field: WasmMemory) : WasmExport<WasmMemory>(name, field, 0x2, "memory")
    class Global(name: String, field: WasmGlobal) : WasmExport<WasmGlobal>(name, field, 0x3, "global")
    class Tag(name: String, field: WasmTag) : WasmExport<WasmTag>(name, field, 0x4, "tag")
}

sealed class WasmTypeDeclaration(
    override val name: String
) : WasmNamedModuleField() {
    abstract fun isShared(): Boolean
}

data class WasmFunctionType(
    val parameterTypes: List<WasmType>,
    val resultTypes: List<WasmType>
) : WasmTypeDeclaration("") {
    override fun isShared(): Boolean = false
}

class WasmStructDeclaration(
    name: String,
    val fields: List<WasmStructFieldDeclaration>,
    val superType: WasmSymbolReadOnly<WasmTypeDeclaration>?,
    val isFinal: Boolean,
    private val isShared: Boolean,
) : WasmTypeDeclaration(name) {
    override fun isShared(): Boolean = isShared
}

class WasmArrayDeclaration(
    name: String,
    val field: WasmStructFieldDeclaration,
    private val isShared: Boolean,
) : WasmTypeDeclaration(name) {
    override fun isShared(): Boolean = isShared
}

class WasmStructFieldDeclaration(
    val name: String,
    val type: WasmType,
    val isMutable: Boolean
)

sealed class WasmInstr(
    val operator: WasmOp,
    val immediates: List<WasmImmediate> = emptyList()
) {
    abstract val location: SourceLocation?
}

class WasmInstrWithLocation(
    operator: WasmOp,
    immediates: List<WasmImmediate>,
    override val location: SourceLocation
) : WasmInstr(operator, immediates) {
    constructor(
        operator: WasmOp,
        location: SourceLocation
    ) : this(operator, emptyList(), location)
}

class WasmInstrWithoutLocation(
    operator: WasmOp,
    immediates: List<WasmImmediate> = emptyList(),
) : WasmInstr(operator, immediates) {
    override val location: SourceLocation? get() = null
}

data class WasmLimits(
    val minSize: UInt,
    val maxSize: UInt?
)

data class WasmImportDescriptor(
    val moduleName: String,
    val declarationName: WasmSymbolReadOnly<String>
)

data class JsBuiltinDescriptor(
    val moduleName: String,
    val declarationName: String,
    val polyfillImpl: String
)
