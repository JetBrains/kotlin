package a

open class ProtectedParent {
    protected open fun inherit() {}
}

class <caret>ProtectedChild : ProtectedParent() {
    override fun inherit() {
        super.inherit()
    }
}