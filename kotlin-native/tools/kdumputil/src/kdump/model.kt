package kdump

import base.Endianness

enum class IdSize {
    BITS_8,
    BITS_16,
    BITS_32,
    BITS_64,
}

enum class RuntimeType {
    OBJECT,
    INT_8,
    INT_16,
    INT_32,
    INT_64,
    FLOAT_32,
    FLOAT_64,
    NATIVE_PTR,
    BOOLEAN,
    VECTOR_128,
}

@JvmInline
value class Id(val long: Long)

data class MemoryDump(
        val headerString: String,
        val endianness: Endianness,
        val idSize: IdSize,
        val items: List<Item>,
)

sealed class Item

data class Type(
        val id: Id,
        val superTypeId: Id,
        val packageName: String,
        val relativeName: String,
        val body: Body,
) : Item() {
    sealed class Body {
        data class Object(
                val instanceSize: Int,
                val objectOffsets: IntArray,
                val debug: Debug?,
        ) : Body() {
            data class Debug(val fields: List<Field>)
        }

        data class Array(
                val elementSize: Int,
                val isObjectArray: Boolean,
                val debug: Debug?,
        ) : Body() {
            data class Debug(val elementType: RuntimeType)
        }
    }

    val isArray: Boolean get() = body is Body.Array

    // TODO: Remove when not needed
    val fields: List<Field>?
        get() = when (body) {
            is Body.Object -> body.debug?.fields
            is Body.Array -> null
        }
}

data class ObjectItem(
        val id: Id,
        val typeId: Id,
        val byteArray: ByteArray,
) : Item()

data class ArrayItem(
        val id: Id,
        val typeId: Id,
        val count: Int,
        val byteArray: ByteArray,
) : Item()

data class ExtraObject(
        val id: Id,
        val baseObjectId: Id,
        val associatedObjectId: Id,
) : Item()

data class Field(
        val offset: Int,
        val type: RuntimeType,
        val name: String,
)

data class Thread(
        val id: Id,
) : Item()

data class GlobalRoot(
        val source: Source,
        val objectId: Id,
) : Item() {
    enum class Source { GLOBAL, STABLE_REF }
}

data class ThreadRoot(
        val threadId: Id,
        val source: Source,
        val objectId: Id,
) : Item() {
    enum class Source { STACK, THREAD_LOCAL }
}
