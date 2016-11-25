package foo


external val classes: Map<String, Any> = noImpl

external val classesMutable: HashMap<String, String> = noImpl

fun box(): String {
    classesMutable.set("why", "?")
    return if (classes.get("answer") == 42 && classesMutable.get("why") == "?") "OK" else "fail"
}