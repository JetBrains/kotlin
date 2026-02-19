package kdump.hprof

import kdump.RuntimeType
import kdump.Type
import hprof.Type as HProfType

fun String.primitiveArrayClassNameToElementTypePair(): Pair<RuntimeType, HProfType> =
        when (this) {
            "String" -> RuntimeType.INT_16 to HProfType.CHAR
            "ByteArray" -> RuntimeType.INT_8 to HProfType.BYTE
            "ShortArray" -> RuntimeType.INT_16 to HProfType.SHORT
            "IntArray" -> RuntimeType.INT_32 to HProfType.INT
            "LongArray" -> RuntimeType.INT_64 to HProfType.LONG
            "FloatArray" -> RuntimeType.FLOAT_32 to HProfType.FLOAT
            "DoubleArray" -> RuntimeType.FLOAT_64 to HProfType.DOUBLE
            "CharArray" -> RuntimeType.INT_16 to HProfType.CHAR
            "BooleanArray" -> RuntimeType.BOOLEAN to HProfType.BOOLEAN
            else -> throw IllegalArgumentException("Invalid primitive array class name $this")
        }

val Type.hprofClassName: String
    get() = hprofMappedClassName ?: hprofDefaultClassName

val Type.hprofMappedClassName: String?
    get() =
        when (packageName) {
            "kotlin" ->
                when (relativeName) {
                    "String" -> if (SYNTHESIZE_JAVA_LANG_STRINGS) ClassName.STRING else ClassName.Array.CHAR
                    "Array" -> ClassName.Array.OBJECT
                    "BooleanArray" -> ClassName.Array.BOOLEAN
                    "CharArray" -> ClassName.Array.CHAR
                    "ByteArray" -> ClassName.Array.BYTE
                    "ShortArray" -> ClassName.Array.SHORT
                    "IntArray" -> ClassName.Array.INT
                    "LongArray" -> ClassName.Array.LONG
                    "FloatArray" -> ClassName.Array.FLOAT
                    "DoubleArray" -> ClassName.Array.DOUBLE
                    else -> null
                }
            else -> null
        }

val Type.hprofDefaultClassName: String
    get() =
        hprofPackageName + hprofPackageSeparator + hprofRelativeName

val Type.hprofPackageName: String
    get() =
        packageName.replace(".", "/")

val Type.hprofPackageSeparator: String
    get() =
        if (packageName.isEmpty()) "" else "/"

val Type.hprofRelativeName: String
    get() =
        relativeName.replace(".", "$")

val RuntimeType.hprofTypes: List<HProfType>
    get() =
        when (this) {
            RuntimeType.OBJECT -> listOf(HProfType.OBJECT)
            RuntimeType.INT_8 -> listOf(HProfType.BYTE)
            RuntimeType.INT_16 -> listOf(HProfType.SHORT)
            RuntimeType.INT_32 -> listOf(HProfType.INT)
            RuntimeType.INT_64 -> listOf(HProfType.LONG)
            RuntimeType.FLOAT_32 -> listOf(HProfType.FLOAT)
            RuntimeType.FLOAT_64 -> listOf(HProfType.DOUBLE)
            RuntimeType.NATIVE_PTR -> listOf(HProfType.LONG)
            RuntimeType.BOOLEAN -> listOf(HProfType.BOOLEAN)
            RuntimeType.VECTOR_128 -> List(4) { HProfType.INT }
        }
