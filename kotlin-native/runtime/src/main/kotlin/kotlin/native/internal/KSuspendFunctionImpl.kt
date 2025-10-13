/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.internal.UsedFromCompilerGeneratedCode

@UsedFromCompilerGeneratedCode
internal abstract class KSuspendFunctionImpl<out R>(description: KFunctionDescription): KFunctionImpl<R>(description) {
    override fun toString(): String {
        val nameStrict = description.checkCorrect().name
        return "suspend function $nameStrict"
    }
}
