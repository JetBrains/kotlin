package hprof

object Binary {
    object Type {
        const val OBJECT = 2
        const val BOOLEAN = 4
        const val CHAR = 5
        const val FLOAT = 6
        const val DOUBLE = 7
        const val BYTE = 8
        const val SHORT = 9
        const val INT = 10
        const val LONG = 11
    }

    object Profile {
        object Record {
            object Tag {
                const val STRING_CONSTANT = 0x01
                const val LOAD_CLASS = 0x02
                const val STACK_FRAME = 0x04
                const val STACK_TRACE = 0x05
                const val START_THREAD = 0x0a
                const val HEAP_DUMP = 0x0c
                const val HEAP_DUMP_SECTION = 0x1c
                const val HEAP_DUMP_END = 0x2c
            }
        }
    }

    object HeapDump {
        object Record {
            object Tag {
                const val ROOT_UNKNOWN = 0xff
                const val ROOT_JNI_GLOBAL = 0x01
                const val ROOT_JNI_LOCAL = 0x02
                const val ROOT_JAVA_FRAME = 0x03
                const val ROOT_STICKY_CLASS = 0x05
                const val ROOT_THREAD_OBJECT = 0x08
                const val CLASS_DUMP = 0x20
                const val INSTANCE_DUMP = 0x21
                const val OBJECT_ARRAY_DUMP = 0x22
                const val PRIMITIVE_ARRAY_DUMP = 0x23
            }
        }
    }
}
