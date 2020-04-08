package shouldFail

import util.*

fun f() {
    val c = funWithUnspecifiedType()
    c + 3
}
