// "Create expected class in common module testModule_Common" "true"
// DISABLE-ERRORS

class A<T> {
    actual inner class <caret>B<F : T>
}