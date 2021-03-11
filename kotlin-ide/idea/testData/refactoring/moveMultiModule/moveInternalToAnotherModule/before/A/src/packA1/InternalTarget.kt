package packA1

import packA2.InternalContentUser

class <caret>InternalContent {
    internal fun internalFun() {}

    fun useInternalInside() {
        internalFun()
    }

    fun useInternal() {
        InternalContentUser().internalFun()
    }
}

class More