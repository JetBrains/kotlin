package b

import a.ProtectedParent

class ProtectedChild : ProtectedParent() {
    fun foo() {
        this.inherit()
        inherit()
    }
}