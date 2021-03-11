// IS_APPLICABLE: false
// WITH_RUNTIME
fun test() {
    J().<caret>setR(fun() { 
        println("Hello, world!")
    })
}