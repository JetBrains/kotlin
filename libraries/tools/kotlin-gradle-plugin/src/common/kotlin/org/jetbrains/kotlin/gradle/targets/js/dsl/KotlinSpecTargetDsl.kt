/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.gradle.api.Action

interface KotlinSpecDsl : KotlinWasmSpecDsl {
}

interface KotlinTargetWithSpecDsl {
    fun spec() = spec { }

    fun spec(body: KotlinSpecDsl.() -> Unit)

    fun spec(fn: Action<KotlinSpecDsl>) {
        spec {
            fn.execute(this)
        }
    }
}
