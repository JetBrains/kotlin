package x64

import kotlinx.cinterop.*
import platform.posix.*

fun test() {
    val file: CPointer< /* NAVIGATION-TARGET:typealias FILE = */ FILE> = /* NAVIGATION-TARGET:external fun fopen */ fopen("file.txt", "r") ?: return
    /* NAVIGATION-TARGET:external fun fprintf */ fprintf(null, "")
    memScoped {
        val addr: CPointerVarOf<CPointer< /* NAVIGATION-TARGET:final class sockaddr_in */ sockaddr_in>> = allocPointerTo()
    }
}
