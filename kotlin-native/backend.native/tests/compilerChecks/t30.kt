import kotlinx.cinterop.*
import platform.darwin.*
import platform.Foundation.*

class Zzz : NSAssertionHandler() {
    @ObjCOutlet
    var NSObject.x: NSObject
        get() = this
        set(value: NSObject) { }
}
