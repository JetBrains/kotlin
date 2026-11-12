// DUMP_LINE_MAPPINGS

// FILE: test.kt
package test

enum class Mode {
    SINGLE,
    MULTI_SELECT,
    CUSTOM {
        override fun label() = "custom"
    };

    open fun label() = name
}

abstract class Drawer {
    var fragmented: Boolean = false
        private set

    var shown: Boolean = false
        protected set

    var opened: Boolean = false
        protected set(value) { field = value }
}

enum class SortOrder(val code: String) {
    ASC("asc"),
    DESC("desc") {
        override fun toString() = code
    }
}

// FILE: nonFatal.kt
@file:JvmName("NonFatalKt")
@file:JvmMultifileClass
package test

fun nonFatal(t: Throwable): Boolean = true
