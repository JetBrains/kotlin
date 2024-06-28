// EXPECTED_REACHABLE_NODES: 1419


fun Throwable.className() = this::class.simpleName!!

class O : Error()
class K : Error()

fun box(): String {
    val o = O()
    val k = K()
    return o.className() + k.className()
}