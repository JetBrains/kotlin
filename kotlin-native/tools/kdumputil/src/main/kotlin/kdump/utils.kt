package kdump

val IdSize.byteCount: Int
  get() =
    when (this) {
      IdSize.BITS_8 -> 1
      IdSize.BITS_16 -> 2
      IdSize.BITS_32 -> 4
      IdSize.BITS_64 -> 8
    }

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

val Item.idOrNull: Long?
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

val MemoryDump.idToItemMap: Map<Long, Item>
  get() =
    items.asSequence().mapNotNull { item -> item.idOrNull?.let { id -> id to item } }.toMap()