/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

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
