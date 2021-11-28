@file:Suppress("unused")

import appleHelper.appleHelper
import nativeHelper.nativeHelper
import unixHelper.unixHelper

object IosMain {
    val native = nativeHelper()
    val unix = unixHelper()
    val apple = appleHelper()
}
