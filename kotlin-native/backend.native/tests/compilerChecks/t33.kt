import kotlinx.cinterop.*
import platform.darwin.*
import platform.Foundation.*

class Zzz : NSString {
    @OverrideInit
    constructor(coder: NSCoder) { }

    override fun initWithCoder(coder: NSCoder): String? = "zzz"
}
