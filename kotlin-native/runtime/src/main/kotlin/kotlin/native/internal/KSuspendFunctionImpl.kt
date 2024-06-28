/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.reflect.KType
import kotlin.reflect.KFunction
import kotlin.reflect.KClass

internal abstract class KSuspendFunctionImpl<out R>(description: KFunctionDescription): KFunctionImpl<R>(description) {
    override fun toString() = "suspend function $name"
}
