// IS_APPLICABLE: false
// WITH_RUNTIME

fun foo() {
    JavaClass.method().toTypedArray<caret><String?>()
}