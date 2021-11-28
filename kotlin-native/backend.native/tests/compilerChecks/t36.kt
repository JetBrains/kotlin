import kotlinx.cinterop.*
import platform.posix.*

fun foo() = stat(malloc(42)!!.rawValue)
