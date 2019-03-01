/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.interop

import com.eclipsesource.v8.V8Object

class V8GlobalContext(val myState: V8Object) : InteropGlobalContext {

    override fun updateState(state: InteropGlobalContext) {
        val v8State = state as V8GlobalContext

        for (key in myState.keys) {
            val value = state.myState.get(key)
            println("CURRENTLY PROCESSING ${key} ${value}")
            when {
                value is Boolean -> myState.add(key, value)
                value is Double -> myState.add(key, value)
                value is Int -> myState.add(key, value)
                value is String -> myState.add(key, value)
                value is V8Object -> myState.add(key, value)
                value == null -> myState.addUndefined(key)
                else -> {
                    println("undefining something which is actually ${value::class.simpleName}")
                    myState.addUndefined(key)
                }
            }
        }
    }

}