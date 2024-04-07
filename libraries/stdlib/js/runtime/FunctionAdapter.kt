/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlin.js

import kotlin.Function

internal interface FunctionAdapter {
    fun getFunctionDelegate(): Function<*>
}