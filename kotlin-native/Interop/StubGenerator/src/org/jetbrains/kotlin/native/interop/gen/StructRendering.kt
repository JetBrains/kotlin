package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.*

fun tryRenderStructOrUnion(def: StructDef): String? = when (def.kind) {
    StructDef.Kind.STRUCT -> tryRenderStruct(def)
    StructDef.Kind.UNION -> tryRenderUnion(def)
    StructDef.Kind.CLASS -> null
}

/**
 * Members of anonymous struct/union are the fields of enclosing named aggregate and has the corresponding offset.
 * However for the purpose of alignment heuristic we use "immediate" offset, i.e. relative to the immediate parent.
 * Consider for ex. a packed struct containing not packed anonymous `Inner`: inner fields are not aligned relative to the root.
 *
 * For the purpose of `isPacked` heuristic we should analyze immediate children only, i.e. ignore the members of nested
 * anonymous struct / union (included by `fields` getter). For ex. inner anon struct may be packed and its members unaligned,
 * however this does not imply `packed` attribute at outer struct.
 *
 * Empty inner records (ie offsetBytes == null) does not affect `packed` heuristic and shall be ignored here.
 * Unsupported members (ie BitField and IncompleteField) to be ignored too but won't be compiled anyway.
 */
private val StructDef.isPacked: Boolean
    get() {
        val baseOffset = fields.firstOrNull()?.offsetBytes ?: return false
        return members.any { member ->
            when (member) {
                is Field -> (member.offsetBytes - baseOffset) % member.typeAlign != 0L
                is AnonymousInnerRecord ->
                    member.offsetBytes?.let { (it - baseOffset) % member.def.align != 0L } ?: false
                else -> false
            }
        }
    }

private fun tryRenderStruct(def: StructDef): String? {
    // The only case when offset starts from non-zero is a inner anonymous struct or union
    val baseOffset = def.fields.firstOrNull()?.offsetBytes ?: 0L
    var offset = 0L

    val isPackedStruct = def.isPacked

    // The following is to deal with the case when a field has big alignment but occasionally its offset is naturally aligned,
    // so we can't guess it by heuristic. However the enclosing struct must be explicitly aligned.
    val maxAlign = def.members.filterIsInstance<Field>().maxOfOrNull { it.typeAlign }
    val forceAlign = maxAlign?.let { def.align > maxAlign }
            ?: (def.align > 1)  // Anonymous inner may be empty AND explicitly aligned

    return buildString {
        append("struct  { ")

        def.members.forEach { it ->
            val decl = when (it) {
                is Field -> {
                    val immediateOffset = it.offsetBytes - baseOffset
                    val defaultAlignment = if (isPackedStruct) 1L else it.typeAlign
                    val alignment = guessAlignment(offset, immediateOffset, defaultAlignment) ?: return null
                    offset = immediateOffset + it.typeSize

                    tryRenderVar(it.type, it.name)
                            ?.plus(if (alignment == defaultAlignment) "" else " __attribute__((aligned($alignment)))")
                }

                is BitField, // TODO: tryRenderVar(it.type, name)?.plus(" : ${it.size}")
                is IncompleteField -> null // e.g. flexible array member.
                is AnonymousInnerRecord -> {
                    // No need to advance offset if offsetBytes is null 'cause it means that record is empty. Assert that.
                    assert(it.offsetBytes != null || it.typeSize == 0L)
                    it.offsetBytes?.let { offsetBytes ->
                        offset = offsetBytes - baseOffset + it.typeSize
                    }
                    tryRenderStructOrUnion(it.def)
                }
            } ?: return null
            append("$decl; ")
        }
        append("}")
        if (isPackedStruct) append(" __attribute__((packed))")
        if (forceAlign) append(" __attribute__((aligned(${def.align})))")
    }
}

private fun guessAlignment(offset: Long, paddedOffset: Long, defaultAlignment: Long): Long? =
        longArrayOf(defaultAlignment, 1L, 2L, 4L, 8L, 16L, 32L).firstOrNull {
            alignUp(offset, it) == paddedOffset
        }

private fun alignUp(x: Long, alignment: Long): Long = (x + alignment - 1) and ((alignment - 1).inv())

private fun tryRenderUnion(def: StructDef): String? {
    val maxAlign = def.members.filterIsInstance<Field>().maxOfOrNull { it.typeAlign }
    val forceAlign = maxAlign?.let { def.align > maxAlign }
            ?: (def.align > 1)  // Anonymous inner may be empty AND explicitly aligned

    return buildString {
        append("union { ")
        def.members.forEach { it ->
            val name = it.name
            val decl = when (it) {
                is Field -> tryRenderVar(it.type, name)
                is BitField, is IncompleteField -> null
                is AnonymousInnerRecord -> tryRenderStructOrUnion(it.def)
            } ?: return null
            append("$decl; ")
        }
        append("}")
        if (forceAlign) append(" __attribute__((aligned(${def.align})))")
    }
}

private fun tryRenderVar(type: Type, name: String): String? = when (type) {
    CharType, is BoolType -> "char $name"
    is IntegerType -> "${type.spelling} $name"
    is FloatingType -> "${type.spelling} $name"
    is VectorType -> "${type.spelling} $name"
    is RecordType -> tryRenderStructOrUnion(type.decl.def!!)?.let { "$it $name" }
    is EnumType -> tryRenderVar(type.def.baseType, name)
    is PointerType -> "void* $name"
    is ConstArrayType -> tryRenderVar(type.elemType, "$name[${type.length}]")
    is IncompleteArrayType -> tryRenderVar(type.elemType, "$name[]")
    is Typedef -> tryRenderVar(type.def.aliased, name)
    is ObjCPointer -> "void* $name"
    else -> null
}

private val Field.offsetBytes: Long
    get() {
        require(this.offset % 8 == 0L)
        return this.offset / 8
    }

private val AnonymousInnerRecord.offsetBytes: Long?
    get() {
        return def.fields.firstOrNull()?.offsetBytes
    }
