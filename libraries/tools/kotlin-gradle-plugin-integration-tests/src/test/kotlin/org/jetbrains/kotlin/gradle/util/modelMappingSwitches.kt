/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.jetbrains.kotlin.gradle.BaseGradleIT

internal inline fun BaseGradleIT.ignoreFailureWithModelMapping(reason: String, action: () -> Unit) {
    if (isKpmModelMappingEnabled) {
        try {
            action()
        } catch (t: Throwable) {
            return
        }
        throw AssertionError("Expected a failure with KPM Model Mapping: $reason. No failure occurred, though. Please review the test!")
    } else {
        action()
    }
}

internal inline fun BaseGradleIT.runWithModelMappingDisabled(@Suppress("UNUSED_PARAMETER") reason: String? = null, action: () -> Unit) {
    val originalValue = isKpmModelMappingEnabled
    try {
        isKpmModelMappingEnabled = false
        action()
    } finally {
        isKpmModelMappingEnabled = originalValue
    }
}

internal inline fun BaseGradleIT.ifModelMappingUsed(@Suppress("UNUSED_PARAMETER") reason: String? = null, action: () -> Unit) {
    if (isKpmModelMappingEnabled)
        action()
}

internal inline fun BaseGradleIT.unlessModelMappingUsed(@Suppress("UNUSED_PARAMETER") reason: String? = null, action: () -> Unit) {
    if (!isKpmModelMappingEnabled)
        action()
}

internal inline fun <T> BaseGradleIT.ifModelMappingUsedOrElse(ifUsed: () -> T, orElse: () -> T, @Suppress("UNUSED_PARAMETER")reason: String? = null, ): T =
    if (isKpmModelMappingEnabled)
        ifUsed()
    else orElse()

internal fun <T> BaseGradleIT.ifModelMappingUsedOrElse(ifUsed: T, orElse: T, reason: String? = null): T =
    ifModelMappingUsedOrElse({ ifUsed }, { orElse }, reason)