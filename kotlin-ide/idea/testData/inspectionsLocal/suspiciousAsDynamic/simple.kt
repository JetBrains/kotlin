// JS
// PROBLEM: Suspicious 'asDynamic' member invocation
// FIX: Remove 'asDynamic' invocation
fun test(d: dynamic) {
    d.<caret>asDynamic().foo()
}