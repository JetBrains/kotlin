package dependency

import dependency.impl.KImpl

open class K {
    companion object {
        fun getInstance(): KImpl = KImpl()
    }
}
