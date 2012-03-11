package js.debug

import js.*

native
val console : consoleClass = js.noImpl

native
class consoleClass() {
    fun log(message : Any?) : Unit = js.noImpl
}