// WITH_RUNTIME
// INTENTION_TEXT: "Add '@JvmOverloads' annotation to secondary constructor"

class A {
    constructor(a: String = ""<caret>, b: Int)
}