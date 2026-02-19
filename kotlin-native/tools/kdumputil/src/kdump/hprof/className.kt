package kdump.hprof

object ClassName {
    object Primitive {
        const val BOOLEAN = "Z"
        const val CHAR = "C"
        const val BYTE = "B"
        const val SHORT = "S"
        const val INT = "I"
        const val LONG = "J"
        const val FLOAT = "F"
        const val DOUBLE = "D"
    }

    const val OBJECT = "java/lang/Object"
    const val STRING = "java/lang/String"
    const val CLASS = "java/lang/Class"
    const val CLASS_LOADER = "java/lang/ClassLoader"
    const val THREAD = "java/lang/Thread"
    const val EXTRA_OBJECT = "kotlin/ExtraObject"

    object Array {
        const val OBJECT = "[L" + ClassName.OBJECT + ";"
        const val BOOLEAN = "[" + ClassName.Primitive.BOOLEAN
        const val CHAR = "[" + ClassName.Primitive.CHAR
        const val BYTE = "[" + ClassName.Primitive.BYTE
        const val SHORT = "[" + ClassName.Primitive.SHORT
        const val INT = "[" + ClassName.Primitive.INT
        const val LONG = "[" + ClassName.Primitive.LONG
        const val FLOAT = "[" + ClassName.Primitive.FLOAT
        const val DOUBLE = "[" + ClassName.Primitive.DOUBLE
    }
}
