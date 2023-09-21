
fun checkIfMySwiftClass(a: Any): Boolean {
    val className = a::class.toString()
    return className.endsWith("MySwiftClass")
}

fun checkIfSwiftValue(a: Any): Boolean {
    val className = a::class.toString()
    return className.endsWith("__SwiftValue")
}

fun checkIfNSTaggedPointerString(a: Any): Boolean {
    val className = a::class.toString()
    return className.endsWith("NSTaggedPointerString")
}

fun areTypesTheSame(a: Any, b: Any): Boolean {
    return a::class == b::class
}