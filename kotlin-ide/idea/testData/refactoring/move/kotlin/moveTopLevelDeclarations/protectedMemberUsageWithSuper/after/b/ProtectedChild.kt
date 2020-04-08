package b

import a.ProtectedParent

class ProtectedChild : ProtectedParent() {
    override fun inherit() {
        super.inherit()
    }
}