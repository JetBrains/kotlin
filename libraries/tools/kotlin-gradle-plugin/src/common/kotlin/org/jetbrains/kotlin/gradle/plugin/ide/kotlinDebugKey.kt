/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.extrasKeyOf

internal val kotlinDebugKey = extrasKeyOf<Any>("kotlin.debug")

val Extras.kotlinDebug get() = this[kotlinDebugKey]

var MutableExtras.kotlinDebug: Any?
    get() = this[kotlinDebugKey]
    set(value) {
        if (value != null) this[kotlinDebugKey] = value
        else this.remove(kotlinDebugKey)
    }
