package includedLib

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun linuxMain() {
    cinterop.a.a()
    @OptIn(ExperimentalForeignApi::class)
    memScoped {
        alloc<cinterop.a.StructA>()
    }
}