import kotlinx.cinterop.*
import platform.darwin.*
import platform.Foundation.*

class Zzz : NSAssertionHandler() {
    @ObjCOutlet
    var x: String 
        get() = "zzz"
        set(value: String) { }
}
