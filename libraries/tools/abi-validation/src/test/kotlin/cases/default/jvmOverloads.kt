/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER")

package cases.default


@JvmOverloads
public fun publicFunWithOverloads(a: Int = 0, b: String? = null) {}

@JvmOverloads
internal fun internalFunWithOverloads(a: Int = 0, b: String? = null) {}

public class JvmOverloadsClass
@JvmOverloads
internal constructor(val a: Int = 0, val b: String? = null) {

    @JvmOverloads
    internal fun internalFunWithOverloads(a: Int = 0, b: String? = null) {}

}