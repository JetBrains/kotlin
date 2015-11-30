package foo

import java.util.HashMap

@native
val classes: Map<String, Any> = noImpl
@native
val classesMutable: HashMap<String, String> = noImpl

fun box(): Boolean {
    classesMutable.set("why", "?")
    return classes.get("answer") == 42 && classesMutable.get("why") == "?"
}