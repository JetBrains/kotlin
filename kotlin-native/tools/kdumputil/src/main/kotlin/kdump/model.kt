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

data class MemoryDump(
  val headerString: String,
  val endianness: Endianness,
  val idSize: IdSize,
  val items: List<Item>,
)

sealed class Item

data class Type(
  val id: Long,
  val superTypeId: Long,
  val packageName: String,
  val relativeName: String,
  val body: Body,
) : Item() {
  sealed class Body {
    data class Object(val instanceSize: Int, val extra: Extra?) : Body() {
      data class Extra(val fields: List<Field>)
    }

    data class Array(val elementSize: Int, val extra: Extra?) : Body() {
        val fakeExtra get() = when(elementSize) {
            1 -> RuntimeType.INT_8
            2 -> RuntimeType.INT_16
            4 -> RuntimeType.INT_32
            8 -> RuntimeType.INT_64
            else -> error("Unexpected element size: $elementSize")
        }
      data class Extra(val elementType: RuntimeType)
    }
  }

  val isArray: Boolean get() = body is Body.Array

  // TODO: Remove when not needed
  val fields: List<Field>?
    get() = when (body) {
      is Body.Object -> body.extra?.fields
      is Body.Array -> null
    }
}

data class ObjectItem(
  val id: Long,
  val typeId: Long,
  val byteArray: ByteArray,
) : Item()

data class ArrayItem(
  val id: Long,
  val typeId: Long,
  val count: Int,
  val byteArray: ByteArray,
) : Item()

data class ExtraObject(
  val id: Long,
  val baseObjectId: Long,
  val associatedObjectId: Long,
) : Item()

data class Field(
  val offset: Int,
  val type: RuntimeType,
  val name: String,
)

data class Thread(
  val id: Long,
) : Item()

data class GlobalRoot(
  val source: Source,
  val objectId: Long,
) : Item() {
  enum class Source { GLOBAL, STABLE_REF }
}

data class ThreadRoot(
  val threadId: Long,
  val source: Source,
  val objectId: Long,
) : Item() {
  enum class Source { STACK, THREAD_LOCAL }
}
