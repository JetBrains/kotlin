import bar.*

import foo.AllOpenGenerated

fun box(): String {
    AllOpenGenerated.NestedB().materialize()
    return "OK"
}
