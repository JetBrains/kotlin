class AFirst : ZMiddle()

class BBox<T>(val value: T)

open class ZMiddle {
    fun zt(): ZT = ZT()
}

@ObjCName("T", exact = true)
class ZT
