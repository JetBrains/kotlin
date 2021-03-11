// PROBLEM: "Use of setter method instead of property access syntax"
// WITH_RUNTIME
fun foo(thread: Thread) {
    thread.setName<caret>("name")
}