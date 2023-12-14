/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

inline fun inlineFun(param: String, lambda: (String) -> String = { it }): String {
    return lambda(param)
}

fun box(): String {
    return inlineFun("OK")
}
