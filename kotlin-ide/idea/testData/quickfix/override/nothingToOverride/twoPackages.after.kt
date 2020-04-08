// "Change function signature to 'fun f(a: A)'" "true"
// ERROR: 'f' overrides nothing
import a.A
import a.B
class A {}
class BB : B() {
    override fun f(a: A) {}
}
