package bar

import foo.CrExtended
import foo.funExtension

fun test() {
    CrExtended().funExtension()
}

// WITH_RUNTIME