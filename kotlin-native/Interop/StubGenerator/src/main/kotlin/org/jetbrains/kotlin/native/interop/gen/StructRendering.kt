package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.*

fun tryRenderStructOrUnion(def: StructDef): String? = when (def.kind) {
    StructDef.Kind.STRUCT -> tryRenderStruct(def)
    StructDef.Kind.UNION -> tryRenderUnion(def)
    StructDef.Kind.CLASS -> null
}

private fun tryRenderStruct(def: StructDef): String? {
    val isPackedStruct = def.fields.any { !it.isAligned }

    var offset = 0L

    return buildString {
        append("struct")
        if (isPackedStruct) append(" __attribute__((packed))")
        append(" { ")

        def.members.forEachIndexed { index, it ->
            val name = "p$index"
            val decl = when (it) {
                is Field -> {
                    val defaultAlignment = if (isPackedStruct) 1L else it.typeAlign
                    val alignment = guessAlignment(offset, it.offsetBytes, defaultAlignment) ?: return null

                    offset = it.offsetBytes + it.typeSize

                    tryRenderVar(it.type, name)
                            ?.plus(if (alignment == defaultAlignment) "" else "__attribute__((aligned($alignment)))")
                }

                is BitField, // TODO: tryRenderVar(it.type, name)?.plus(" : ${it.size}")
                is IncompleteField -> null // e.g. flexible array member.
            } ?: return null
            append("$decl; ")
        }

        append("}")
    }
}

private fun guessAlignment(offset: Long, paddedOffset: Long, defaultAlignment: Long): Long? =
        longArrayOf(defaultAlignment, 1L, 2L, 4L, 8L, 16L, 32L).firstOrNull {
            alignUp(offset, it) == paddedOffset
        }

private fun alignUp(x: Long, alignment: Long): Long = (x + alignment - 1) and ((alignment - 1).inv())

private fun tryRenderUnion(def: StructDef): String? =
        if (def.members.any { it.offset != 0L }) null else buildString {
            append("union { ")
            def.members.forEachIndexed { index, it ->
                val decl = when (it) {
                    is Field -> tryRenderVar(it.type, "p$index")
                    is BitField, is IncompleteField -> null
                } ?: return null

                append("$decl; ")
            }
            append("}")

        }

private fun tryRenderVar(type: Type, name: String): String? = when (type) {
    CharType, is BoolType -> "char $name"
    is IntegerType -> "${type.spelling} $name"
    is FloatingType -> "${type.spelling} $name"
    is VectorType -> "${type.spelling} $name"
    is RecordType -> "${tryRenderStructOrUnion(type.decl.def!!)} $name"
    is EnumType -> tryRenderVar(type.def.baseType, name)
    is PointerType -> "void* $name"
    is ConstArrayType -> tryRenderVar(type.elemType, "$name[${type.length}]")
    is IncompleteArrayType -> tryRenderVar(type.elemType, "$name[]")
    is Typedef -> tryRenderVar(type.def.aliased, name)
    is ObjCPointer -> "void* $name"
    else -> null
}

private val Field.offsetBytes: Long get() {
    require(this.offset % 8 == 0L)
    return this.offset / 8
}