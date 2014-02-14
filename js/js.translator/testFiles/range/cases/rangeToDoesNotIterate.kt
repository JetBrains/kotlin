package foo

import java.util.ArrayList

fun box(): Boolean {
    for (i in 0 rangeTo -1) {
        return false
    }
    return true
}