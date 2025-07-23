// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

class Container {
    val d: String = "OK"
}

class Wrapper {
    var container: Container? = null
}

fun getWrapper(flag: Boolean): Wrapper? {
    return if (flag) Wrapper() else null
}

fun box(): String {
    // Create a potentially null wrapper
    val wrapper = getWrapper(true)
    
    // Access container which is initially null
    val container = wrapper?.container
    
    // Try to access field 'd' on the null container
    // This should be safe in Kotlin due to null safety, but might generate problematic bytecode
    val result = container?.d
    
    // Use a more complex pattern with lambdas and control flow
    val complexResult = wrapper?.let { w ->
        w.container?.let { c ->
            c.d
        }
    }
    
    // Try with nested null checks and assignments
    if (wrapper != null) {
        wrapper.container = Container()
        val c = wrapper.container
        if (c != null) {
            return c.d
        }
    }
    
    return "OK"
}