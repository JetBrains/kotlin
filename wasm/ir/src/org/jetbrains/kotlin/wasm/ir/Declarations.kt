/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

class WasmImportPair(
    val module: String,
    val name: String
)

class WasmModule(
    val functionTypes: List<WasmFunctionType>,
    val structTypes: List<WasmStructType>,
    val importedFunctions: List<WasmImportedFunction>,
    val definedFunctions: List<WasmDefinedFunction>,
    val table: WasmTable,
    val memory: WasmMemory,
    val globals: List<WasmGlobal>,
    val exports: List<WasmExport>,
    val startFunction: WasmStartFunction?,
    val data: List<WasmData>
)

/**
 * Calculate declaration IDs of linked wasm module
 */
fun WasmModule.calculateIds() {
    fun List<WasmNamedModuleField>.calculateIds(startIndex: Int = 0) {
        for ((index, field) in this.withIndex()) {
            field.id = index + startIndex
        }
    }

    functionTypes.calculateIds()
    structTypes.calculateIds(startIndex = functionTypes.size)
    importedFunctions.calculateIds()
    definedFunctions.calculateIds(startIndex = importedFunctions.size)
    globals.calculateIds()
}


class WasmSymbol<T : Any>(owner: T? = null) {
    private var _owner: T? = owner

    val owner: T
        get() = _owner
            ?: error("Unbound wasm symbol $this")

    fun bind(value: T) {
        _owner = value
    }

    override fun equals(other: Any?): Boolean =
        other is WasmSymbol<*> && _owner == other._owner

    override fun hashCode(): Int =
        _owner.hashCode()

    override fun toString(): String =
        _owner?.toString() ?: "Unbound"
}

sealed class WasmNamedModuleField(
    val name: String,
    val prefix: String
) {
    var id: Int? = null
}

sealed class WasmFunction(
    name: String,
    val type: WasmFunctionType
) : WasmNamedModuleField(name, "fun")

class WasmDefinedFunction(
    name: String,
    type: WasmFunctionType,
    val locals: MutableList<WasmLocal> = mutableListOf(),
    val instructions: MutableList<WasmInstr> = mutableListOf()
) : WasmFunction(name, type)

class WasmImportedFunction(
    name: String,
    type: WasmFunctionType,
    val importPair: WasmImportPair
) : WasmFunction(name, type)

class WasmMemory(
    val minSize: Int,
    val maxSize: Int?
)

class WasmData(
    val offset: Int,
    val bytes: ByteArray
)

class WasmTable(
    val functions: List<WasmFunction>
)

class WasmLocal(
    val id: Int,
    val name: String,
    val type: WasmValueType,
    val isParameter: Boolean
)

class WasmGlobal(
    name: String,
    val type: WasmValueType,
    val isMutable: Boolean,
    val init: List<WasmInstr>
) : WasmNamedModuleField(name, "g")

class WasmExport(
    val function: WasmFunction,
    val exportedName: String,
    val kind: Kind
) {
    enum class Kind(val keyword: String) {
        FUNCTION("func"),
    }
}

class WasmStartFunction(val ref: WasmFunction)

sealed class WasmTypeDeclaration(name: String) :
    WasmNamedModuleField(name, "type")

class WasmFunctionType(
    name: String,
    val parameterTypes: List<WasmValueType>,
    val resultType: WasmValueType?
) : WasmTypeDeclaration(name)

class WasmStructType(
    name: String,
    val fields: List<WasmStructFieldDeclaration>
) : WasmTypeDeclaration(name) {
    override fun toString(): String = "(struct $$name)"
}

class WasmStructFieldDeclaration(
    val name: String,
    val type: WasmValueType,
    val isMutable: Boolean
)