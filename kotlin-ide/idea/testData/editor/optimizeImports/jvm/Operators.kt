// NAME_COUNT_TO_USE_STAR_IMPORT: 5
import p1.*

fun f(runnable: Runnable) {
    var r = -runnable
    print(r + 1)
    r /= 2
    r *= 3
}