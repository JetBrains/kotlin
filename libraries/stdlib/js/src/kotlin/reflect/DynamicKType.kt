/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.js.internal

import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

internal object DynamicKType : KType {
    override val classifier: KClassifier? = null
    override val arguments: List<KTypeProjection> = emptyList()
    override val isMarkedNullable: Boolean = false
    override fun toString(): String = "dynamic"
}
