// EXPECTED_REACHABLE_NODES: 493
package foo

fun box(): String {

    var s: String = ""

    try {
        js("throw null")
    } catch (e: Throwable) {
        s = "Throwable"
    } catch (e: dynamic) {
        s = "dynamic"
    }
    assertEquals("dynamic", s)

    s = ""
    try {
        try {
            js("throw null")
        }
        catch (e: Throwable) {
            s = "Throwable"
        }
    } catch (e: dynamic) {
        s = "dynamic"
    }
    assertEquals("dynamic", s)

    s = ""
    try {
        js("throw Object.create(null)")
    }
    catch (e: dynamic) {
        s = "dynamic"
    }
    assertEquals("dynamic", s)

    return "OK"
}