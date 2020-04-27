/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

open class SExpressionBuilder {
    protected val stringBuilder = StringBuilder()
    protected var indent = 0

    protected inline fun indented(body: () -> Unit) {
        indent++
        body()
        indent--
    }

    protected fun newLine() {
        stringBuilder.appendLine()
        repeat(indent) { stringBuilder.append("    ") }
    }

    protected inline fun newLineList(name: String, body: () -> Unit) {
        newLine()
        stringBuilder.append("($name")
        indented { body() }
        stringBuilder.append(")")
    }

    protected inline fun sameLineList(name: String, body: () -> Unit) {
        stringBuilder.append(" ($name")
        body()
        stringBuilder.append(")")
    }

    protected fun appendElement(value: String) {
        stringBuilder.append(" ")
        stringBuilder.append(value)
    }

    override fun toString(): String =
        stringBuilder.toString()
}


class WatBuilder : SExpressionBuilder() {
    fun appendMemoryArgument(memoryArgument: WasmMemoryArgument) {
        if (memoryArgument.offset != 0)
            appendElement("offset=${memoryArgument.offset}")
        if (memoryArgument.align != 0)
            appendElement("align=${memoryArgument.align}")
    }

    fun appendInstr(wasmInstr: WasmInstr) {
        newLine()
        stringBuilder.append(wasmInstr.operator.mnemonic)
        wasmInstr.accept(immediateArgumentBuilder, null)
    }

    private val immediateArgumentBuilder = object : WasmInstrVisitor<Unit, Nothing?> {
        override fun visitConstant(x: WasmConstInstr, data: Nothing?) {
            appendElement(
                when (x) {
                    is WasmConstInstr.I32 -> x.value.toString()
                    is WasmConstInstr.I64 -> x.value.toString()
                    is WasmConstInstr.F32 -> x.value.toString()
                    is WasmConstInstr.F64 -> x.value.toString()
                    is WasmConstInstr.I32Symbol -> x.value.owner.toString()
                    WasmConstInstr.F32NaN -> "nan"
                    WasmConstInstr.F64NaN -> "nan"
                }.toLowerCase()
            )
        }

        override fun visitLoad(x: WasmLoad, data: Nothing?) {
            appendMemoryArgument(x.memoryArgument)
        }

        override fun visitStore(x: WasmStore, data: Nothing?) {
            appendMemoryArgument(x.memoryArgument)
        }

        override fun visitBranchTarget(x: WasmBranchTarget, data: Nothing?) {
            appendElement("$${x.label}")
            val type = x.type
            if (type != null && type !is WasmUnreachableType) {
                sameLineList("result") { appendType(type) }
            }
        }

        override fun visitBr(x: WasmBr, data: Nothing?) {
            appendElement(x.target.toString())
        }

        override fun visitBrIf(x: WasmBrIf, data: Nothing?) {
            appendElement(x.target.toString())
        }

        override fun visitCall(x: WasmCall, data: Nothing?) {
            appendModuleFieldReference(x.symbol.owner)
        }

        override fun visitCallIndirect(x: WasmCallIndirect, data: Nothing?) {
            sameLineList("type") { appendModuleFieldReference(x.symbol.owner) }
        }

        override fun visitGetLocal(x: WasmGetLocal, data: Nothing?) {
            appendLocalReference(x.local)
        }

        override fun visitSetLocal(x: WasmSetLocal, data: Nothing?) {
            appendLocalReference(x.local)
        }

        override fun visitLocalTee(x: WasmLocalTee, data: Nothing?) {
            appendLocalReference(x.local)
        }

        override fun visitGetGlobal(x: WasmGetGlobal, data: Nothing?) {
            appendModuleFieldReference(x.global.owner)
        }

        override fun visitSetGlobal(x: WasmSetGlobal, data: Nothing?) {
            appendModuleFieldReference(x.global.owner)
        }

        override fun visitStructGet(x: WasmStructGet, data: Nothing?) {
            appendModuleFieldReference(x.structName.owner)
            appendElement(x.fieldId.owner.toString())
        }

        override fun visitStructNew(x: WasmStructNew, data: Nothing?) {
            appendModuleFieldReference(x.structName.owner)
        }

        override fun visitStructSet(x: WasmStructSet, data: Nothing?) {
            appendModuleFieldReference(x.structName.owner)
            appendElement(x.fieldId.owner.toString())
        }

        override fun visitStructNarrow(x: WasmStructNarrow, data: Nothing?) {
            appendType(x.fromType)
            appendType(x.type)
        }

        override fun visitInstr(x: WasmInstr, data: Nothing?) {
        }
    }

    fun appendWasmModule(module: WasmModule) {
        with(module) {
            newLineList("module") {
                functionTypes.forEach { appendFunctionTypeDeclaration(it) }
                structTypes.forEach { applendStructTypeDeclaration(it) }
                importedFunctions.forEach { appendImportedFunction(it) }
                definedFunctions.forEach { appendDefinedFunction(it) }
                appendTable(table)
                appendMemory(memory)
                globals.forEach { appendGlobal(it) }
                exports.forEach { appendExport(it) }
                startFunction?.let { appendStartFunction(it) }
                data.forEach { appendData(it) }
            }
        }
    }

    fun appendFunctionTypeDeclaration(type: WasmFunctionType) {
        newLineList("type") {
            appendModuleFieldReference(type)
            sameLineList("func") {
                sameLineList("param") {
                    type.parameterTypes.forEach { appendType(it) }
                }
                type.resultType?.let {
                    sameLineList("result") { appendType(it) }
                }
            }
        }
    }

    fun applendStructTypeDeclaration(type: WasmStructType) {
        newLineList("type") {
            appendModuleFieldReference(type)
            sameLineList("struct") {
                type.fields.forEach {
                    appendStructField(it)
                }
            }
        }
    }

    fun appendImportedFunction(function: WasmImportedFunction) {
        newLineList("func") {
            appendModuleFieldReference(function)
            sameLineList("import") {
                appendElement(toWatString(function.importPair.module))
                appendElement(toWatString(function.importPair.name))
            }
            sameLineList("type") { appendModuleFieldReference(function.type) }
        }
    }

    fun appendDefinedFunction(function: WasmDefinedFunction) {
        newLineList("func") {
            appendModuleFieldReference(function)
            sameLineList("type") { appendModuleFieldReference(function.type) }
            function.locals.forEach { if (it.isParameter) appendLocal(it) }
            function.type.resultType?.let {
                sameLineList("result") { appendType(it) }
            }
            function.locals.forEach { if (!it.isParameter) appendLocal(it) }
            function.instructions.forEach { appendInstr(it) }
        }
    }

    fun appendTable(table: WasmTable) {
        newLineList("table") {
            appendElement("funcref")
            newLineList("elem") {
                table.functions.forEach {
                    newLine()
                    appendModuleFieldReference(it)
                }
            }
        }
    }

    fun appendMemory(memory: WasmMemory) {
        newLineList("memory") {
            appendElement(memory.minSize.toString())
            memory.maxSize?.let { appendElement(memory.maxSize.toString()) }
        }
    }

    fun appendGlobal(global: WasmGlobal) {
        newLineList("global") {
            appendModuleFieldReference(global)

            if (global.isMutable)
                sameLineList("mut") { appendType(global.type) }
            else
                appendType(global.type)

            global.init.forEach { appendInstr(it) }
        }
    }

    fun appendExport(export: WasmExport) {
        newLineList("export") {
            appendElement(toWatString(export.exportedName))
            sameLineList(export.kind.keyword) {
                appendModuleFieldReference(export.function)
            }
        }
    }

    fun appendStartFunction(startFunction: WasmStartFunction) {
        newLineList("start") {
            appendModuleFieldReference(startFunction.ref)
        }
    }

    fun appendData(wasmData: WasmData) {
        newLineList("data") {
            sameLineList("i32.const") { appendElement(wasmData.offset.toString()) }
            appendElement(wasmData.bytes.toWatData())
        }
    }

    fun appendLocal(local: WasmLocal) {
        newLineList(if (local.isParameter) "param" else "local") {
            appendLocalReference(local)
            appendType(local.type)
        }
    }

    fun appendType(type: WasmValueType) {
        when (type) {
            is WasmSimpleValueType -> appendElement(type.mnemonic)
            is WasmStructRef -> sameLineList("optref") { appendModuleFieldReference(type.structType.owner) }
            WasmUnreachableType -> {
            }
        }
    }

    fun appendStructField(field: WasmStructFieldDeclaration) {
        sameLineList("field") {
            if (field.isMutable) {
                sameLineList("mut") { appendType(field.type) }
            } else {
                appendType(field.type)
            }
        }
    }

    fun appendLocalReference(local: WasmLocal) {
        appendElement("$${local.id}_${sanitizeWatIdentifier(local.name)}")
    }

    fun appendModuleFieldReference(field: WasmNamedModuleField) {
        val id = field.id ?: error("${field.prefix} ${field.name} ID is unlinked")
        appendElement("\$${sanitizeWatIdentifier(field.name)}___${field.prefix}_$id")
    }
}

fun toWatString(s: String): String {
    // TODO: escape characters according to
    //  https://webassembly.github.io/spec/core/text/values.html#strings
    return "\"" + s + "\""
}

fun Byte.toWatData() = "\\" + this.toUByte().toString(16).padStart(2, '0')
fun ByteArray.toWatData(): String = "\"" + joinToString("") { it.toWatData() } + "\""

fun sanitizeWatIdentifier(ident: String): String {
    if (ident.isEmpty())
        return "_"
    if (ident.all(::isValidWatIdentifierChar))
        return ident
    return ident.map { if (isValidWatIdentifierChar(it)) it else "_" }.joinToString("")
}

// https://webassembly.github.io/spec/core/text/values.html#text-id
fun isValidWatIdentifierChar(c: Char): Boolean =
    c in '0'..'9' || c in 'A'..'Z' || c in 'a'..'z'
            // TODO: SpiderMonkey js shell can't parse some of the
            //  permitted identifiers: '?', '<'
            // || c in "!#$%&′*+-./:<=>?@\\^_`|~"
            || c in "$.@_"
