package js.debug

import js.*

// https://developer.mozilla.org/en/DOM/console
native
class Console() {
  fun dir(o:Any) = {}
  fun error(vararg o:Any?) = {}
  fun info(vararg o:Any?) = {}
  fun log(vararg o:Any?) = {}
  fun warn(vararg o:Any?) = {}
}

native
val console = Console()