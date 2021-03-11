// TARGET_CLASS: B
open class A {
    open fun callSuper() {
        // Something important
    }
}

open class B : A() {

}

class <caret>C : B() {
    // INFO: {"checked": "true"}
    override fun callSuper() {
        super.callSuper() // We simply call up to the base class
    }
}