package sample

fun jvm() {
    println(common())
    println(<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: js" textAttributesKey="WRONG_REFERENCES_ATTRIBUTES">js</error>())
}