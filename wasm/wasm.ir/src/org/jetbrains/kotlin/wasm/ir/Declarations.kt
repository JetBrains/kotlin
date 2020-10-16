/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir


class WasmModule(
    val functionTypes: List<WasmFunctionType> = emptyList(),
    val structs: List<WasmStructDeclaration> = emptyList(),

    val importsInOrder: List<WasmNamedModuleField> = emptyList(),
    val importedFunctions: List<WasmFunction.Imported> = emptyList(),
    val importedMemories: List<WasmMemory> = emptyList(),
    val importedTables: List<WasmTable> = emptyList(),
    val importedGlobals: List<WasmGlobal> = emptyList(),

    val definedFunctions: List<WasmFunction.Defined> = emptyList(),
    val tables: List<WasmTable> = emptyList(),
    val memories: List<WasmMemory> = emptyList(),
    val globals: List<WasmGlobal> = emptyList(),
    val exports: List<WasmExport<*>> = emptyList(),
    val elements: List<WasmElement> = emptyList(),

    val startFunction: WasmFunction? = null,

    val data: List<WasmData> = emptyList(),
    val dataCount: Boolean = false,
)

sealed class WasmNamedModuleField {
    var id: Int? = null
    open val name: String = ""
}

sealed class WasmFunction(
    override val name: String,
    val type: WasmFunctionType
) : WasmNamedModuleField() {
    class Defined(
        name: String,
        type: WasmFunctionType,
        val locals: MutableList<WasmLocal> = mutableListOf(),
        val instructions: MutableList<WasmInstr> = mutableListOf()
    ) : WasmFunction(name, type)

    class Imported(
        name: String,
        type: WasmFunctionType,
        val importPair: WasmImportPair
    ) : WasmFunction(name, type)
}

class WasmMemory(
    val limits: WasmLimits = WasmLimits(1u, null),
    val importPair: WasmImportPair? = null,
) : WasmNamedModuleField()

sealed class WasmDataMode {
    class Active(
        val memoryIdx: Int,
        val offset: MutableList<WasmInstr>
    ) : WasmDataMode() {
        constructor(memoryIdx: Int, offset: Int) : this(memoryIdx, mutableListOf<WasmInstr>().also<MutableList<WasmInstr>> {
            WasmIrExpressionBuilder(it).buildConstI32(offset)
        })
    }

    object Passive : WasmDataMode()
}

class WasmData(
    val mode: WasmDataMode,
    val bytes: ByteArray,
) : WasmNamedModuleField()

class WasmTable(
    val limits: WasmLimits = WasmLimits(1u, null),
    val elementType: WasmType,
    val importPair: WasmImportPair? = null
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
        class Active(val table: WasmTable, val offset: List<WasmInstr>) : Mode()
        object Declarative : Mode()
    }
}

class WasmLocal(
    val id: Int,
    val name: String,
    val type: WasmType,
    val isParameter: Boolean
)

class WasmGlobal(
    override val name: String,
    val type: WasmType,
    val isMutable: Boolean,
    val init: List<WasmInstr>,
    val importPair: WasmImportPair? = null
) : WasmNamedModuleField()

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
}

sealed class WasmTypeDeclaration(
    override val name: String
) : WasmNamedModuleField()

class WasmFunctionType(
    name: String,
    val parameterTypes: List<WasmType>,
    val resultTypes: List<WasmType>
) : WasmTypeDeclaration(name)

class WasmStructDeclaration(
    name: String,
    val fields: List<WasmStructFieldDeclaration>
) : WasmTypeDeclaration(name)

class WasmStructFieldDeclaration(
    val name: String,
    val type: WasmType,
    val isMutable: Boolean
)

class WasmInstr(
    val operator: WasmOp,
    val immediates: List<WasmImmediate> = emptyList()
)

data class WasmLimits(
    val minSize: UInt,
    val maxSize: UInt?
)

data class WasmImportPair(
    val moduleName: String,
    val declarationName: String
)