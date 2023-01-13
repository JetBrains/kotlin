// EXPECTED_REACHABLE_NODES: 1403
// MODULE_KIND: COMMON_JS

package foo

@JsExport
interface Foo

class Bar: Foo

fun box(): String {
    val foo : Foo = Bar()

    if(foo is Collection<*>) {
        return "Fail: Foo is a Collection!"
    }

    return "OK"
}