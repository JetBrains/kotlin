// EXPECTED_REACHABLE_NODES: 1226
package foo

class Ex1(val s: String) : Exception()
class Ex2(val x: Ex1) : Exception()

fun box(): String {

    var s: String = ""

    try {
        throw Ex1("OK")
    } catch (e: RuntimeException) {
        s = "Failed1"
    } catch (e: Ex1) {
        try {
            throw Ex2(e)
        } catch (r: RuntimeException) {
            s = "Failed2"
        } catch (ex: Ex2){
            s = ex.x.s
        }
    }

    return s
}