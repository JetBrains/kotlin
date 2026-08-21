// Soundness: break/continue target enclosing loops; unreachable blocks pruned.

fun box(): String {
    var i = 0
    while (i < 3) {
        i++
        if (i == 2) break
        if (i == 1) continue
    }
    return if (i >= 2) "OK" else "FAIL"
}
