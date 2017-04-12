@file:JvmName("MultifileKt")
@file:JvmMultifileClass
package cases.private


// const
private const val privateConst: Int = 4

// fun
@Suppress("UNUSED_PARAMETER")
private fun privateFun(x: Any) {}


private class PrivateClassInMultifile {
    internal fun accessUsage() {
        privateFun(privateConst)
    }

}
