import platform.darwin.*
import platform.Foundation.*

class Zzz : NSAssertionHandler() {
    override fun handleFailureInFunction(functionName: String, file: String, lineNumber: NSInteger /* = Long */, description: String?, vararg args: Any?) { }
}
