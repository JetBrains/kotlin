@file:Suppress("unused")

import appleHelper.appleHelper
import nativeHelper.nativeHelper
import unixHelper.unixHelper


object MacosMain {
    val native = nativeHelper()
    val unix = unixHelper()
    val apple = appleHelper()
}
