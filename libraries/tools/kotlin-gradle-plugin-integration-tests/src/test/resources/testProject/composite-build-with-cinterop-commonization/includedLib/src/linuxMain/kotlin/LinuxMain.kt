package includedLib

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped

fun linuxMain() {
    cinterop.a.a()
    @OptIn(ExperimentalForeignApi::class)
    memScoped {
        alloc<cinterop.a.StructA>()
    }
}