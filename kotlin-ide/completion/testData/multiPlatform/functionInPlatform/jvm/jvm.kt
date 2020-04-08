package test

actual fun foo() {

}

fun use() {
    foo<caret>
}

// EXIST: { lookupString: foo, module: testModule_JVM }
// ABSENT: { lookupString: foo, module: testModule_Common }