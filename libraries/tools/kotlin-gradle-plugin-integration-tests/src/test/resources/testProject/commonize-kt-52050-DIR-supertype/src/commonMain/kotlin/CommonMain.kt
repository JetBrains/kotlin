@file:Suppress("UNUSED_PARAMETER")

import kotlinx.cinterop.CPointed
import platform.posix.DIR

fun requireCPointed(value: CPointed): Unit = error("Not supported")
fun DIR(): DIR = error("Not supported")

fun main() {
    /*
    Linux and Windows Hosts failed to retain
    CPointed supertype of `DIR` when commonizing.
    This happened in the special case of 'single platform propagation', which
    lost forward declarations from one step to the next.
     */
    requireCPointed(DIR())
}
