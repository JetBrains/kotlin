/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir.convertors

import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.debug.DebugData
import org.jetbrains.kotlin.wasm.ir.debug.DebugInformation
import org.jetbrains.kotlin.wasm.ir.debug.DebugInformationConsumer
import org.jetbrains.kotlin.wasm.ir.debug.DebugInformationGenerator
import org.jetbrains.kotlin.wasm.ir.source.location.*

private object WasmBinary {
    const val MAGIC = 0x6d736100u
    const val VERSION = 1u

    // https://webassembly.github.io/spec/core/binary/instructions.html#control-instructions
    const val EMPTY_TYPE_FOR_BLOCK: Byte = -0x40 // 0x40

    // TODO Change the link to final spec when it's merged
    // https://webassembly.github.io/gc/core/binary/types.html
    const val FUNC_TYPE: Byte = -0x20 // 0x60
    const val STRUCT_TYPE: Byte = -0x21 // 0x5F
    const val ARRAY_TYPE: Byte = -0x22 // 0x5E
    const val SUB_TYPE: Byte = -0x30 // 0x50
    const val SUB_FINAL_TYPE: Byte = -0x31 // 0x4F
    const val REC_GROUP: Byte = -0x32 // 0x4E

    @JvmInline
    value class Section private constructor(val id: UShort) {
        companion object {
            // https://webassembly.github.io/spec/core/binary/modules.html#sections
            val CUSTOM = Section(0u)
            val TYPE = Section(1u)
            val IMPORT = Section(2u)
            val FUNCTION = Section(3u)
            val TABLE = Section(4u)
            val MEMORY = Section(5u)
            val GLOBAL = Section(6u)
            val EXPORT = Section(7u)
            val START = Section(8u)
            val ELEMENT = Section(9u)
            val CODE = Section(10u)
            val DATA = Section(11u)
            val DATA_COUNT = Section(12u)
            val TAG = Section(13u)
        }
    }
}

private enum class AnnotationKind(val sectionName: String) {
    BRANCH_HINT("metadata.code.branch_hint"),
    TRACE_INST("metadata.code.trace_inst"),
    JS_CALLED("binaryen.js.called"),
}

/**
 * Annotation data extracted from pseudo-instructions during code emission.
 * Stores the kind, byte offset, and raw payload value for later section generation.
 */
private class ResolvedAnnotation(
    val kind: AnnotationKind,
    val byteOffset: Int,
    val value: Int = 0,
)

/**
 * Collected resolved annotations for a function, ready for section emission.
 */
private class FunctionResolvedAnnotations(
    val functionIndex: Int,
    val annotations: List<ResolvedAnnotation>
)

class WasmIrToBinary(
    val b: ByteWriterWithOffsetWrite,
    val module: WasmModule,
    val moduleName: String,
    val emitNameSection: Boolean,
    private val debugInformationGenerator: DebugInformationGenerator? = null,
) : DebugInformationConsumer {
    private var codeSectionOffset = Box(0)
    private val appendImmediateDelegate = ::appendImmediate
    private val defaultEndInstruction = wasmInstrWithoutLocation(WasmOp.END)
    private val resolver = module.resolver

    // Collected resolved annotations per function for code metadata section emission
    private val resolvedAnnotationsByFunction = mutableListOf<FunctionResolvedAnnotations>()

    // Context for tracking annotations during function emission
    private var currentFunctionCodeStart: Int = -1
    private var currentFunctionAnnotations: MutableList<ResolvedAnnotation>? = null

    override fun consumeDebugInformation(debugInformation: DebugInformation) {
        debugInformation.forEach {
            appendSection(WasmBinary.Section.CUSTOM) {
                b.writeString(it.name)
                when (it.data) {
                    is DebugData.StringData -> b.writeString(it.data.value)
                    is DebugData.RawBytes -> b.writeBytes(it.data.value)
                }
            }
        }
    }

    private fun appendWasmTypeList(typeList: List<WasmTypeDeclaration>) {
        typeList.forEach { type ->
            when (type) {
                is WasmStructDeclaration -> appendStructTypeDeclaration(type)
                is WasmArrayDeclaration -> appendArrayTypeDeclaration(type)
                is WasmFunctionType -> appendFunctionTypeDeclaration(type)
            }
        }
    }

    fun appendWasmModule() {
        b.writeUInt32(WasmBinary.MAGIC)
        b.writeUInt32(WasmBinary.VERSION)

        with(module) {
            // type section
            appendSection(WasmBinary.Section.TYPE) {
                appendVectorSize(recGroups.size)
                recGroups.forEach { recGroup ->
                    if (recGroup.size > 1) {
                        b.writeVarInt7(WasmBinary.REC_GROUP)
                        appendVectorSize(recGroup.size)
                        appendWasmTypeList(recGroup)
                    } else {
                        appendWasmTypeList(recGroup)
                    }
                }
            }

            // import section
            appendSection(WasmBinary.Section.IMPORT) {
                appendVectorSize(importsInOrder.size)
                importsInOrder.forEach {
                    when (it) {
                        is WasmFunction.Imported -> appendImportedFunction(it)
                        is WasmMemory -> appendMemory(it)
                        is WasmTable -> appendTable(it)
                        is WasmGlobal -> appendGlobal(it)
                        is WasmTag -> appendTag(it)
                        else -> error("Unknown import kind ${it::class}")
                    }
                }
            }

            // function section
            appendSection(WasmBinary.Section.FUNCTION) {
                appendVectorSize(definedFunctions.size)
                definedFunctions.forEach { appendDefinedFunction(it) }
            }

            // table section
            appendSection(WasmBinary.Section.TABLE) {
                appendVectorSize(tables.size)
                tables.forEach { appendTable(it) }
            }

            // memory section
            appendSection(WasmBinary.Section.MEMORY) {
                appendVectorSize(memories.size)
                memories.forEach { appendMemory(it) }
            }

            // tag section
            if (tags.isNotEmpty()) {
                appendSection(WasmBinary.Section.TAG) {
                    appendVectorSize(tags.size)
                    tags.forEach { appendTag(it) }
                }
            }

            appendSection(WasmBinary.Section.GLOBAL) {
                appendVectorSize(globals.size)
                globals.forEach { appendGlobal(it) }
            }

            appendSection(WasmBinary.Section.EXPORT) {
                appendVectorSize(exports.size)
                exports.forEach { appendExport(it) }
            }

            if (startFunction != null) {
                appendSection(WasmBinary.Section.START) {
                    appendStartFunction(startFunction)
                }
            }

            // element section
            appendSection(WasmBinary.Section.ELEMENT) {
                appendVectorSize(elements.size)
                elements.forEach { appendElement(it) }
            }

            if (dataCount) {
                appendSection(WasmBinary.Section.DATA_COUNT) {
                    b.writeVarUInt32(data.size.toUInt())
                }
            }

            // code section
            appendSection(WasmBinary.Section.CODE) {
                appendVectorSize(definedFunctions.size)
                val importedFunctionCount = importedFunctions.size
                definedFunctions.forEachIndexed { index, func ->
                    appendCode(func, importedFunctionCount + index)
                }
            }

            // code metadata sections
            //
            // Note: We generate the code metadata sections after the code section is appended
            // because the metadata sections depend on the annotations processed in the instruction
            // stream. However,
            // https://github.com/WebAssembly/branch-hinting/blob/main/proposals/branch-hinting/Overview.md
            // specifies that the section comes before. This is actually not a problem for us,
            // because binaryen itself reorders the section if necessary.
            emitCodeMetadataSections()

            appendSection(WasmBinary.Section.DATA) {
                appendVectorSize(data.size)
                data.forEach { appendData(it) }
            }

            // text section (should be placed after data)
            if (emitNameSection) {
                appendTextSection(definedFunctions)
            }

            debugInformationGenerator?.let { consumeDebugInformation(it.generateDebugInformation()) }
        }
    }

    /**
     * Emit code metadata sections for all collected annotations.
     */
    private fun emitCodeMetadataSections() {
        if (resolvedAnnotationsByFunction.isEmpty()) return

        // Group all annotations by kind
        val byKind = mutableMapOf<AnnotationKind, MutableList<Pair<Int, ResolvedAnnotation>>>()
        for (funcAnnotations in resolvedAnnotationsByFunction) {
            for (resolved in funcAnnotations.annotations) {
                byKind.getOrPut(resolved.kind) { mutableListOf() }
                    .add(funcAnnotations.functionIndex to resolved)
            }
        }

        // Emit a custom section for each annotation type in the format described here:
        // https://github.com/WebAssembly/tool-conventions/blob/main/CodeMetadata.md
        for ((kind, entries) in byKind) {
            appendSection(WasmBinary.Section.CUSTOM) {
                b.writeString(kind.sectionName)

                // Group by function index
                val byFunction = entries.groupBy({ it.first }, { it.second })
                    .toSortedMap()

                b.writeVarUInt32(byFunction.size)

                for ((funcIdx, annotations) in byFunction) {
                    b.writeVarUInt32(funcIdx)
                    b.writeVarUInt32(annotations.size)

                    for (resolved in annotations.sortedBy { it.byteOffset }) {
                        b.writeVarUInt32(resolved.byteOffset)
                        withVarUInt32PayloadSizePrepended {
                            when (kind) {
                                AnnotationKind.BRANCH_HINT -> {
                                    b.writeVarUInt32(resolved.value)
                                }
                                AnnotationKind.TRACE_INST -> {
                                    b.writeVarInt32(resolved.value)
                                }
                                AnnotationKind.JS_CALLED -> {}
                            }
                        }
                    }
                }
            }
        }
    }

    private fun appendTextSection(definedFunctions: List<WasmFunction.Defined>) {
        appendSection(WasmBinary.Section.CUSTOM) {
            b.writeString("name")
            appendSection(WasmBinary.Section.CUSTOM) {
                b.writeString(moduleName)
            }
            appendSection(WasmBinary.Section.TYPE) {
                appendVectorSize(definedFunctions.size)
                definedFunctions.forEach {
                    appendModuleFieldReference(it)
                    b.writeString(it.name)
                }
            }
            appendSection(WasmBinary.Section.IMPORT) {
                appendVectorSize(definedFunctions.size)
                definedFunctions.forEach {
                    appendModuleFieldReference(it)
                    appendVectorSize(it.locals.size)
                    it.locals.forEach { local ->
                        b.writeVarUInt32(local.id)
                        b.writeString(local.name)
                    }
                }
            }

            // Extended Name Section
            // https://github.com/WebAssembly/extended-name-section/blob/main/document/core/appendix/custom.rst

            appendSection(WasmBinary.Section.TABLE) {
                appendVectorSize(module.recGroups.sumOf { it.size })
                module.recGroups.forEach { recGroup ->
                    recGroup.forEach {
                        appendModuleFieldReference(it)
                        b.writeString(it.name)
                    }
                }
            }

            appendSection(WasmBinary.Section.EXPORT) {
                appendVectorSize(module.globals.size)
                module.globals.forEach { global ->
                    appendModuleFieldReference(global)
                    b.writeString(global.name)
                }
            }

            // Experimental fields name section
            // https://github.com/WebAssembly/gc/issues/193
            appendSection(WasmBinary.Section.CODE) {
                val structDeclarations = module.recGroups.flatMap { it.filterIsInstance<WasmStructDeclaration>() }
                appendVectorSize(structDeclarations.size)
                structDeclarations.forEach {
                    appendModuleFieldReference(it)
                    appendVectorSize(it.fields.size)
                    it.fields.forEachIndexed { index, field ->
                        b.writeVarUInt32(index)
                        b.writeString(field.name)
                    }
                }
            }
        }
    }

    private fun appendInstr(instr: WasmInstr) {
        instr.location?.let {
            debugInformationGenerator?.addSourceLocation(getCurrentSourceLocationMapping(it))
        }

        val opcode = instr.operator.opcode

        if (opcode == WASM_OP_PSEUDO_OPCODE) {
            // Handle annotation pseudo-instructions by recording them without emitting bytes
            require(currentFunctionCodeStart != -1) { "Annotation context not set up." }
            val annotations = currentFunctionAnnotations ?: mutableListOf<ResolvedAnnotation>().also { currentFunctionAnnotations = it }
            val byteOffset = b.written - currentFunctionCodeStart
            when (instr.operator) {
                WasmOp.PSEUDO_ANNOTATION_BRANCH_HINT -> {
                    val likely = (instr.firstImmediateOrNull()!! as WasmImmediate.ConstU8).value.toInt()
                    annotations.add(ResolvedAnnotation(AnnotationKind.BRANCH_HINT, byteOffset, likely))
                }
                WasmOp.PSEUDO_ANNOTATION_TRACE_INST -> {
                    val markId = (instr.firstImmediateOrNull()!! as WasmImmediate.ConstI32).value
                    annotations.add(ResolvedAnnotation(AnnotationKind.TRACE_INST, byteOffset, markId))
                }
                WasmOp.PSEUDO_ANNOTATION_JS_CALLED -> {
                    require(byteOffset == 0) { "js.called annotation must be emitted at function start." }
                    annotations.add(ResolvedAnnotation(AnnotationKind.JS_CALLED, byteOffset))
                }
                WasmOp.PSEUDO_COMMENT_PREVIOUS_INSTR -> {}
                WasmOp.PSEUDO_COMMENT_GROUP_START -> {}
                WasmOp.PSEUDO_COMMENT_GROUP_END -> {}
                else -> error("Unknown annotation pseudo-instruction: ${instr.operator}")
            }
            return
        }

        if (opcode > 0xFF) {
            b.writeByte((opcode ushr 8).toByte())
            b.writeByte((opcode and 0xFF).toByte())
        } else {
            b.writeByte(opcode.toByte())
        }

        instr.forEachImmediates(appendImmediateDelegate)
    }

    private fun getCurrentSourceLocationMapping(sourceLocation: SourceLocation): SourceLocationMappingToBinary =
        SourceLocationMappingToBinary(
            sourceLocation = sourceLocation,
            codeSectionOffset = codeSectionOffset,
            offset = b.written
        )

    private fun appendImmediate(x: WasmImmediate) {
        when (x) {
            is WasmImmediate.ConstU8 -> b.writeUByte(x.value)
            is WasmImmediate.ConstI32 -> b.writeVarInt32(x.value)
            is WasmImmediate.ConstI64 -> b.writeVarInt64(x.value)
            is WasmImmediate.ConstF32 -> b.writeUInt32(x.rawBits)
            is WasmImmediate.ConstF64 -> b.writeUInt64(x.rawBits)
            is WasmImmediate.SymbolI32 -> b.writeVarInt32(x.value.owner)
            is WasmImmediate.MemArg -> {
                b.writeVarUInt32(x.align)
                b.writeVarUInt32(x.offset)
            }
            is WasmImmediate.BlockType -> appendBlockType(x)
            is WasmImmediate.FuncIdx -> appendModuleFieldReference(resolver.resolve(x))
            is WasmImmediate.LocalIdx -> appendLocalReference(x.value)
            is WasmImmediate.GlobalIdx -> appendModuleFieldReference(resolver.resolve(x))
            is WasmImmediate.TypeIdx -> appendModuleFieldReference(resolver.resolve(x))

            is WasmImmediate.MemoryIdx -> b.writeVarUInt32(x.value)
            is WasmImmediate.DataIdx -> b.writeVarUInt32(x.value.owner)
            is WasmImmediate.TableIdx -> b.writeVarUInt32(x.value.owner)
            is WasmImmediate.LabelIdx -> b.writeVarUInt32(x.value)
            is WasmImmediate.TagIdx -> b.writeVarUInt32(x.value.owner)
            is WasmImmediate.LabelIdxVector -> {
                b.writeVarUInt32(x.value.size)
                for (target in x.value) {
                    b.writeVarUInt32(target)
                }
            }
            is WasmImmediate.ElemIdx -> appendModuleFieldReference(x.value)
            is WasmImmediate.ValTypeVector -> {
                b.writeVarUInt32(x.value.size)
                for (type in x.value) {
                    appendType(type)
                }
            }
            is WasmImmediate.StructFieldIdx -> b.writeVarUInt32(x.value)
            is WasmImmediate.HeapType -> appendHeapType(x.value)
            is WasmImmediate.ConstString ->
                error("Instructions with pseudo immediates should be skipped")

            is WasmImmediate.Catch -> {
                b.writeVarUInt32(x.type.opcode)
                x.immediates.forEach(this::appendImmediate)
            }
        }
    }

    private inline fun appendSection(section: WasmBinary.Section, content: () -> Unit) {
        b.writeVarUInt7(section.id)
        codeSectionOffset = Box(-1)
        codeSectionOffset.value = withVarUInt32PayloadSizePrepended(content)
    }

    private inline fun withVarUInt32PayloadSizePrepended(fn: () -> Unit): Int {
        val prependOffset = b.written //offset for placeholder
        b.writeVarUInt32Fixed(0) //placeholder for size
        val prependStart = b.written

        fn()

        val size = b.written - prependStart
        b.writeVarUInt32FixedSize(size, prependOffset) //placeholder for size

        return prependStart
    }

    private fun appendVectorSize(size: Int) {
        b.writeVarUInt32(size)
    }

    private fun appendFunctionTypeDeclaration(type: WasmFunctionType) {
        b.writeVarInt7(WasmBinary.FUNC_TYPE)
        b.writeVarUInt32(type.parameterTypes.size)
        type.parameterTypes.forEach { appendType(it) }
        b.writeVarUInt32(type.resultTypes.size)
        type.resultTypes.forEach { appendType(it) }
    }

    private fun appendBlockType(type: WasmImmediate.BlockType) {
        when (type) {
            is WasmImmediate.BlockType.Function -> {
                val field = type.type.owner
                val id = field.id
                    ?: error("${field::class} ${field.name} ID is unlinked")
                b.writeVarInt32(id)
            }
            is WasmImmediate.BlockType.Value -> when (type.type) {
                null -> b.writeVarInt7(WasmBinary.EMPTY_TYPE_FOR_BLOCK)
                else -> appendType(type.type)
            }
        }
    }

    private fun appendFieldType(field: WasmStructFieldDeclaration) {
        appendType(field.type)
        b.writeVarUInt1(field.isMutable)
    }

    private fun appendStructTypeDeclaration(type: WasmStructDeclaration) {
        val superType = type.superType

        // https://webassembly.github.io/gc/core/binary/types.html#binary-subtype
        if (superType == null && type.isFinal) {
            // Short encoding form for final types without subtypes.
        } else {
            // General encoding
            b.writeVarInt7(
                if (type.isFinal)
                    WasmBinary.SUB_FINAL_TYPE
                else
                    WasmBinary.SUB_TYPE
            )

            if (superType != null) {
                appendVectorSize(1)
                val superType = resolver.resolve(superType)
                appendModuleFieldReference(superType)
            } else {
                appendVectorSize(0)
            }
        }

        b.writeVarInt7(WasmBinary.STRUCT_TYPE)
        b.writeVarUInt32(type.fields.size)
        type.fields.forEach {
            appendFieldType(it)
        }
    }

    private fun appendArrayTypeDeclaration(type: WasmArrayDeclaration) {
        b.writeVarInt7(WasmBinary.ARRAY_TYPE)
        appendFieldType(type.field)
    }

    private fun appendLimits(limits: WasmLimits) {
        b.writeVarUInt1(limits.maxSize != null)
        b.writeVarUInt32(limits.minSize)
        if (limits.maxSize != null)
            b.writeVarUInt32(limits.maxSize)
    }

    private fun appendImportedFunction(function: WasmFunction.Imported) {
        b.writeString(function.importPair.moduleName)
        b.writeString(function.importPair.declarationName.owner)
        b.writeByte(0)  // Function external kind.
        b.writeVarUInt32((resolver.resolve(function.type).id!!))
    }

    private fun appendDefinedFunction(function: WasmFunction.Defined) {
        b.writeVarUInt32((resolver.resolve(function.type).id!!))
    }

    private fun appendTable(table: WasmTable) {
        if (table.importPair != null) {
            b.writeString(table.importPair.moduleName)
            b.writeString(table.importPair.declarationName.owner)
            b.writeByte(1)
        }

        appendType(table.elementType)
        appendLimits(table.limits)
    }

    private fun appendMemory(memory: WasmMemory) {
        if (memory.importPair != null) {
            b.writeString(memory.importPair.moduleName)
            b.writeString(memory.importPair.declarationName.owner)
            b.writeByte(2)
        }
        appendLimits(memory.limits)
    }

    private fun appendGlobal(c: WasmGlobal) {
        if (c.importPair != null) {
            b.writeString(c.importPair.moduleName)
            b.writeString(c.importPair.declarationName.owner)
            b.writeByte(3)
            appendType(c.type)
            b.writeVarUInt1(c.isMutable)
            return
        }
        appendType(c.type)
        b.writeVarUInt1(c.isMutable)
        appendExpr(c.init)
    }

    private fun appendTag(t: WasmTag) {
        if (t.importPair != null) {
            b.writeString(t.importPair.moduleName)
            b.writeString(t.importPair.declarationName.owner)
            b.writeByte(4)
        }
        b.writeByte(0) // attribute

        val tagType = resolver.resolve(t.type) as WasmFunctionType
        check(tagType.resultTypes.isEmpty()) { "Must have empty return as per current spec" }
        val typeId = tagType.id
        check(typeId != null) { "Unlinked tag id" }
        b.writeVarUInt32(typeId)
    }

    private fun appendExpr(expr: Iterable<WasmInstr>, endLocation: SourceLocation? = null) {
        expr.forEach {
            appendInstr(it)
        }
        val endInstr = endLocation?.let { wasmInstrWithLocation(WasmOp.END, it) } ?: defaultEndInstruction
        appendInstr(endInstr)
    }

    private fun appendExport(export: WasmExport<*>) {
        b.writeString(export.name)
        b.writeByte(export.kind)
        appendModuleFieldReference(export.field)
    }

    private fun appendStartFunction(startFunction: WasmFunction) {
        appendModuleFieldReference(startFunction)
    }

    private fun appendElement(element: WasmElement) {
        val isFuncIndices = element.values.all { it is WasmTable.Value.Function } && element.type == WasmFuncRef

        val funcIndices = if (isFuncIndices) {
            element.values.map { (it as WasmTable.Value.Function).function.owner.id!! }
        } else null

        fun writeElements() {
            appendVectorSize(element.values.size)
            if (funcIndices != null) {
                funcIndices.forEach { b.writeVarUInt32(it) }
            } else {
                element.values.forEach {
                    appendExpr((it as WasmTable.Value.Expression).expr)
                }
            }
        }

        fun writeTypeOrKind() {
            if (isFuncIndices) {
                b.writeByte(0x00)
            } else {
                appendType(element.type)
            }
        }

        when (val mode = element.mode) {
            WasmElement.Mode.Passive -> {
                b.writeByte(if (isFuncIndices) 0x01 else 0x05)
                writeTypeOrKind()
                writeElements()
            }
            is WasmElement.Mode.Active -> {
                val tableId = mode.table.id!!
                when {
                    tableId == 0 && isFuncIndices -> {
                        b.writeByte(0x0)
                        appendExpr(mode.offset)
                    }
                    isFuncIndices -> {
                        b.writeByte(0x2)
                        appendModuleFieldReference(mode.table)
                        appendExpr(mode.offset)
                        writeTypeOrKind()
                    }
                    else -> {
                        b.writeByte(0x6)
                        appendModuleFieldReference(mode.table)
                        appendExpr(mode.offset)
                        writeTypeOrKind()
                    }
                }
                writeElements()
            }
            WasmElement.Mode.Declarative -> {
                b.writeByte(if (isFuncIndices) 0x03 else 0x07)
                writeTypeOrKind()
                writeElements()
            }
        }
    }

    private fun appendCode(function: WasmFunction.Defined, functionIndex: Int) {
        val shouldWriteLocationBeforeFunctionHeader = function.endLocation is SourceLocation.IgnoredLocation

        if (shouldWriteLocationBeforeFunctionHeader) {
            debugInformationGenerator?.addSourceLocation(
                SourceLocationMappingToBinary(
                    sourceLocation = SourceLocation.IgnoredLocation,
                    codeSectionOffset = codeSectionOffset,
                    offset = b.written
                )
            )
        }

        withVarUInt32PayloadSizePrepended {
            if (!shouldWriteLocationBeforeFunctionHeader) {
                debugInformationGenerator?.addSourceLocation(
                    getCurrentSourceLocationMapping(SourceLocation.NextLocation)
                )
            }

            // Code annotation offsets are relative to the first byte
            // of the locals.
            currentFunctionCodeStart = b.written

            b.writeVarUInt32(function.locals.count { !it.isParameter })
            function.locals.forEach { local ->
                if (!local.isParameter) {
                    b.writeVarUInt32(1u)
                    appendType(local.type)
                }
            }

            debugInformationGenerator?.startFunction(getCurrentSourceLocationMapping(function.startLocation), function.name)
            appendExpr(function.instructions, function.endLocation)

            // Clear annotation tracking context; save any collected annotations
            val annotations = currentFunctionAnnotations
            currentFunctionAnnotations = null
            currentFunctionCodeStart = -1

            // Store resolved annotations for later section emission
            if (annotations != null) {
                resolvedAnnotationsByFunction.add(FunctionResolvedAnnotations(functionIndex, annotations))
            }

            debugInformationGenerator?.endFunction(getCurrentSourceLocationMapping(function.endLocation))
        }
    }

    private fun appendData(wasmData: WasmData) {
        when (val mode = wasmData.mode) {
            is WasmDataMode.Active -> {
                if (mode.memoryIdx == 0) {
                    b.writeByte(0)
                } else {
                    b.writeByte(2)
                    b.writeVarUInt32(mode.memoryIdx)
                }
                appendExpr(mode.offset)
            }
            WasmDataMode.Passive -> b.writeByte(1)
        }

        b.writeVarUInt32(wasmData.bytes.size)
        b.writeBytes(wasmData.bytes)
    }

    fun appendHeapType(type: WasmHeapType) {
        val code: Int = when (type) {
            is WasmHeapType.Simple -> type.code.toInt()
            is WasmHeapType.Type -> resolver.resolve(type).id!!
        }
        b.writeVarInt32(code)
    }

    fun appendType(type: WasmType) {
        b.writeVarInt7(type.code)

        if (type is WasmRefType) {
            appendHeapType(type.heapType)
        }
        if (type is WasmRefNullType) {
            appendHeapType(type.heapType)
        }
    }

    fun appendLocalReference(local: Int) {
        b.writeVarUInt32(local)
    }

    fun appendModuleFieldReference(field: WasmNamedModuleField) {
        val id = field.id
            ?: error("${field::class} ${field.name} ID is unlinked")
        b.writeVarUInt32(id)
    }

    fun ByteWriter.writeVarUInt32(v: Int) {
        this.writeVarUInt32(v.toUInt())
    }

    fun ByteWriter.writeVarUInt32Fixed(v: Int) {
        this.writeVarUInt32FixedSize(v.toUInt())
    }

    private fun ByteWriter.writeString(str: String) {
        val bytes = str.toByteArray()
        this.writeVarUInt32(bytes.size)
        this.writeBytes(bytes)
    }


    private class SourceLocationMappingToBinary(
        override val sourceLocation: SourceLocation,
        private val codeSectionOffset: Box,
        private val offset: Int
    ) : SourceLocationMapping() {

        override val generatedLocation: SourceLocation.DefinedLocation
            get() = SourceLocation.DefinedLocation(
                file = "",
                line = 0,
                column = offset
            )

        override val generatedLocationRelativeToCodeSection: SourceLocation.DefinedLocation
            get() = SourceLocation.DefinedLocation(
                file = "",
                line = 0,
                column = offset - codeSectionOffset.value
            )
    }
}
