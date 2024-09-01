package kdump

import base.toLongUnsigned

val IdSize.byteCount: Int
    get() =
        when (this) {
            IdSize.BITS_8 -> 1
            IdSize.BITS_16 -> 2
            IdSize.BITS_32 -> 4
            IdSize.BITS_64 -> 8
        }

fun id(byte: Byte) = Id(byte.toLongUnsigned())
fun id(short: Short) = Id(short.toLongUnsigned())
fun id(int: Int) = Id(int.toLongUnsigned())
fun id(long: Long) = Id(long)

fun RuntimeType.size(idSize: IdSize): Int =
        when (this) {
            RuntimeType.OBJECT -> idSize.byteCount
            RuntimeType.INT_8 -> 1
            RuntimeType.INT_16 -> 2
            RuntimeType.INT_32 -> 4
            RuntimeType.INT_64 -> 8
            RuntimeType.FLOAT_32 -> 4
            RuntimeType.FLOAT_64 -> 8
            RuntimeType.NATIVE_PTR -> 8
            RuntimeType.BOOLEAN -> 1
            RuntimeType.VECTOR_128 -> 16
        }

fun Item.size(idSize: IdSize): Int? =
        when (this) {
            is Type -> idSize.byteCount * 8                       // some approximation
            is ObjectItem -> idSize.byteCount + byteArray.size    // type + body
            is ArrayItem -> idSize.byteCount * 2 + byteArray.size // type + count + body
            is ExtraObject -> idSize.byteCount * 3                // type + 2 fields
            is Thread -> idSize.byteCount * 8                     // some approximation
            is GlobalRoot -> null
            is ThreadRoot -> null
        }

val Type.isKotlinString: Boolean
    get() =
        packageName == "kotlin" && relativeName == "String"

val Item.idOrNull: Id?
    get() =
        when (this) {
            is Type -> id
            is ObjectItem -> id
            is ArrayItem -> id
            is ExtraObject -> id
            is Thread -> id
            is GlobalRoot -> null
            is ThreadRoot -> null
        }

val MemoryDump.idToItemMap: Map<Id, Item>
    get() =
        items.asSequence().mapNotNull { item -> item.idOrNull?.let { id -> id to item } }.toMap()

private fun buildFieldsFromObjectOffsets(
        objectOffsets: IntArray,
        objectSize: Int,
        idSize: IdSize,
): List<Field> {
    var offset = 0
    var dataFieldIndex = 0
    var objectFieldIndex = 0

    return buildList {
        fun addObjectField() {
            add(Field(offset, RuntimeType.OBJECT, "object_$objectFieldIndex"))
            offset += idSize.byteCount
            objectFieldIndex += 1
        }

        fun addDataFields(nextOffset: Int) {
            do {
                val delta = nextOffset - offset
                if (delta == 0) break
                when {
                    delta >= 8 ->
                        add(Field(offset, RuntimeType.INT_64, "data_$dataFieldIndex")).also { offset += 8 }

                    delta >= 4 ->
                        add(Field(offset, RuntimeType.INT_32, "data_$dataFieldIndex")).also { offset += 4 }

                    delta >= 2 ->
                        add(Field(offset, RuntimeType.INT_16, "data_$dataFieldIndex")).also { offset += 2 }

                    else ->
                        add(Field(offset, RuntimeType.INT_8, "data_$dataFieldIndex")).also { offset += 1 }
                }
                dataFieldIndex += 1
            } while (true)
        }

        objectOffsets.sortedArray().forEach { objectOffset ->
            addDataFields(objectOffset)
            addObjectField()
        }

        addDataFields(objectSize)
    }
}

fun Type.Body.Object.buildSyntheticFields(idSize: IdSize): List<Field> =
        buildFieldsFromObjectOffsets(objectOffsets, instanceSize, idSize)
