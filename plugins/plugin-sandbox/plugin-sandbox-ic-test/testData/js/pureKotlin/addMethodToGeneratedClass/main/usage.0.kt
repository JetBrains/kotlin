import foo.AllOpenGenerated
import bar.*

fun box(): String {
    AllOpenGenerated.NestedA().materialize()
    return "OK"
}
