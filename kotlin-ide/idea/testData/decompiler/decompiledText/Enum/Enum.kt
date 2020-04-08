package test

import dependency.*

enum class Enum {
    A, B, C, D, E, F {
        override fun f() = 4
    };

    companion object {
        val c: Int = 1
    }

    open fun f() = 3
}
