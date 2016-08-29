package foo

import java.util.HashMap

@native
val classes: Map<String, Any> = noImpl
@native
val classesMutable: HashMap<String, String> = noImpl

fun box(): String {
    classesMutable.set("why", "?")
    return if (classes.get("answer") == 42 && classesMutable.get("why") == "?") "OK" else "fail"
}