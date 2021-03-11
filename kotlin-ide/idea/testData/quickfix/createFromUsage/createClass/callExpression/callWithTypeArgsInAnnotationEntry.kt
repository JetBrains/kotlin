// "Create annotation 'bar'" "true"
// ERROR: Unresolved reference: foo

@[foo(1, "2", <caret>bar<String, Int>("3", 4))] fun test() {

}