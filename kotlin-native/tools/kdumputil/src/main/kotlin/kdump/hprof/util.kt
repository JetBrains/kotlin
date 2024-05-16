package kdump.hprof

import hprof.Type
import kdump.Field
import kdump.RuntimeType

fun String.primitiveArrayClassNameToElementTypePair(): Pair<RuntimeType, Type> =
  when (this) {
    "String" -> RuntimeType.INT_16 to Type.CHAR
    "ByteArray" -> RuntimeType.INT_8 to Type.BYTE
    "ShortArray" -> RuntimeType.INT_16 to Type.SHORT
    "IntArray" -> RuntimeType.INT_32 to Type.INT
    "LongArray" -> RuntimeType.INT_64 to Type.LONG
    "FloatArray" -> RuntimeType.FLOAT_32 to Type.FLOAT
    "DoubleArray" -> RuntimeType.FLOAT_64 to Type.DOUBLE
    "CharArray" -> RuntimeType.INT_16 to Type.CHAR
    "BooleanArray" -> RuntimeType.BOOLEAN to Type.BOOLEAN

    "NativePtrArray" -> RuntimeType.NATIVE_PTR to Type.LONG // FIXME
    else -> throw IllegalArgumentException("Invalid primitive array class name $this")
  }

val kdump.Type.hprofClassName: String
  get() =
    if (!isArray) {
      hprofMappedClassName ?: hprofDefaultClassName
    } else if (packageName == "kotlin") {
      when (relativeName) {
        "String" -> if (ADD_JAVA_LANG_STRINGS) ClassName.STRING else ClassName.Array.CHAR
        "Array" -> ClassName.Array.OBJECT
        "BooleanArray" -> ClassName.Array.BOOLEAN
        "CharArray" -> ClassName.Array.CHAR
        "ByteArray" -> ClassName.Array.BYTE
        "ShortArray" -> ClassName.Array.SHORT
        "IntArray" -> ClassName.Array.INT
        "LongArray" -> ClassName.Array.LONG
        "FloatArray" -> ClassName.Array.FLOAT
        "DoubleArray" -> ClassName.Array.DOUBLE
        else -> throw IllegalArgumentException("Invalid array relative name: $relativeName")
      }
    } else {
      throw IllegalArgumentException("Invalid array package name: $packageName")
    }

val kdump.Type.hprofMappedClassName: String?
  get() =
    when (packageName) {
      "kotlin" ->
        when (relativeName) {
          "String" -> ClassName.STRING
          else -> null
        }

      else -> null
    }

val kdump.Type.hprofDefaultClassName: String
  get() =
    hprofPackageName + hprofPackageSeparator + hprofRelativeName

val kdump.Type.hprofPackageName: String
  get() =
    packageName.replace(".", "/")

val kdump.Type.hprofPackageSeparator: String
  get() =
    if (packageName.isEmpty()) "" else "/"

val kdump.Type.hprofRelativeName: String
  get() =
    relativeName.replace(".", "$")

val kdump.Type.forceFields: List<Field> get() = fields ?: listOf() //throw IOException("No fields")

val RuntimeType.hprofTypes: List<Type>
  get() =
    when (this) {
      RuntimeType.OBJECT -> listOf(Type.OBJECT)
      RuntimeType.INT_8 -> listOf(Type.BYTE)
      RuntimeType.INT_16 -> listOf(Type.SHORT)
      RuntimeType.INT_32 -> listOf(Type.INT)
      RuntimeType.INT_64 -> listOf(Type.LONG)
      RuntimeType.FLOAT_32 -> listOf(Type.FLOAT)
      RuntimeType.FLOAT_64 -> listOf(Type.DOUBLE)
      RuntimeType.NATIVE_PTR -> listOf(Type.LONG)
      RuntimeType.BOOLEAN -> listOf(Type.BOOLEAN)
      RuntimeType.VECTOR_128 -> List(4) { Type.INT }
    }
