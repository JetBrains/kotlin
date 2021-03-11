// "Create expected class in common module testModule_Common" "true"
// DISABLE-ERRORS

class A<T> {
    actual class B<F : T>
    actual class C<F>
    actual class D<F : Any>
    actual class <caret>E<F : Any?>
    actual class G<F : Dwwwq>
}