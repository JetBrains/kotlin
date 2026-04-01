import bar.*
import foo.AllOpenGenerated

fun box(): String {
    AllOpenGenerated.NestedA().materialize()
    AllOpenGenerated.NestedB().materialize()
    AllOpenGenerated.NestedA().materialize()
    return "OK"
}
