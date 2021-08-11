package noAutorelease

class TestNoAutorelease {
    class NoAutorelease {
        val x = 11
    }

    private var obj: NoAutorelease? = NoAutorelease()
    private val weakObj = kotlin.native.ref.WeakReference(obj!!)

    fun returnObj() = obj
    fun clearObj() {
        obj = null
    }

    fun isObjUnreachable(): Boolean {
        kotlin.native.internal.GC.collect()
        return weakObj.value == null
    }
}
