package unsafeCall

// this test is used also to check more than one file for package in JetPositionManager:prepareTypeMapper. see forTests/simple.kt
fun main(args: Array<String>) {
    val s1: String? = "a"
    val s2: String? = null
    //Breakpoint!
    args.size
}

// EXPRESSION: s1.length
// RESULT: 1: I

// EXPRESSION: s2.length
// RESULT: java.lang.NullPointerException