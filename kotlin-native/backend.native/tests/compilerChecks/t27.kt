import kotlinx.cinterop.*
import platform.darwin.*
import platform.Foundation.*

class Zzz : NSAssertionHandler() {
    @ObjCAction
    fun foo(x: String) = println(x)
}
