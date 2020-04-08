fun <T> doSomething(a: T) {}

fun foo() {
    if (true) <caret>{
        //comment
        doSomething("test")
    }
}
