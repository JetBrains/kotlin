// IS_APPLICABLE: false
// ERROR: The feature "multi platform projects" is experimental and should be enabled explicitly
expect class A(a: Int) {
    class B(<caret>b: String)
}