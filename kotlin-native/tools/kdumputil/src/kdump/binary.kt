package kdump

object RecordTag {
    const val TYPE = 0x01
    const val OBJECT = 0x02
    const val ARRAY = 0x03
    const val EXTRA_OBJECT = 0x04
    const val THREAD = 0x05
    const val GLOBAL_ROOT = 0x06
    const val THREAD_ROOT = 0x07
}

object RootSourceTag {
    const val GLOBAL = 0x01
    const val STABLE_REF = 0x02
}

object ThreadRootSourceTag {
    const val STACK = 0x01
    const val THREAD_LOCAL = 0x02
}
