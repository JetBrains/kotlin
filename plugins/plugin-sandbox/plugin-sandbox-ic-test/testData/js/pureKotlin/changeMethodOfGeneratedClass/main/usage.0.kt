import bar.*

import foo.AllOpenGenerated

fun box(): String {
    AllOpenGenerated.NestedA().materialize()
    return "OK"
}
