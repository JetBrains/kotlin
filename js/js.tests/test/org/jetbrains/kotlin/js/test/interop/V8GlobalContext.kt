/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.interop

import com.eclipsesource.v8.V8Object
import com.eclipsesource.v8.utils.V8ObjectUtils

class V8GlobalContext(val myState: V8Object) : InteropGlobalContext {

    override fun updateState(state: Map<String, Any?>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun toMap(): Map<String, Any?> {
        return V8ObjectUtils.toMap(myState)
    }
}