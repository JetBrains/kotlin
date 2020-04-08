// "Create actual class for module testModule_JVM (JVM)" "true"
// SHOULD_FAIL_WITH: Cannot generate class: Type &lt;Unknown&gt; is not accessible from target module
// DISABLE-ERRORS

expect class Foo<caret> {
    var bar
}