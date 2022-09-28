inline fun foo(): Int {
    bar(null)
    return MyClass1.InternalClass.companionMethod() + 1
}

fun bar(x: Any?): Any = (x as? MyClass1) ?: 1
