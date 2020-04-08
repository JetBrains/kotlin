// "Change function signature to 'fun f(a: A)'" "true"
// ERROR: 'f' overrides nothing
import a.A
import a.B

class BB : B() {
    <caret>override fun f(a: A) {}
}
