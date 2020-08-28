/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/**
 * @param CT is return type of calling constructor (uses in DCE)
 */
internal fun <CT> construct(constructorType: dynamic, resultType: dynamic, vararg args: Any?): Any {
    return js("Reflect").construct(constructorType, args, resultType)
}
