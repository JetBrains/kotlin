package foo

import java.util.*

native
val classes: Map<String, Any> = noImpl
native
val classesMutable: HashMap<String, String> = noImpl

fun box(): Boolean {
    classesMutable["why"] = "?"
    return classes["answer"] == 42 && classesMutable["why"] == "?"
}