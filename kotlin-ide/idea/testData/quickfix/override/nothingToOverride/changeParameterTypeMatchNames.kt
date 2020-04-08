// "Change function signature to 'fun f(x: Int, t: String, z: Double)'" "true"
open class A {
    open fun f(x: Int, y: String, z: Double) {}
}

class B : A(){
    <caret>override fun f(z: String, x: String, t: String) {}
}
