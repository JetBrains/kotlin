@file:kotlin.native.internal.objc.BindClassToObjCName(Q::class, "KnOpenQ")

package p

import kotlin.native.internal.objc.BindReverseBridgeToMethod

open class Q {
    open fun tag(): Int = 7
}

@BindReverseBridgeToMethod(Q::class, "tag")
private fun tagRev(self: Q): Int = self.tag()
