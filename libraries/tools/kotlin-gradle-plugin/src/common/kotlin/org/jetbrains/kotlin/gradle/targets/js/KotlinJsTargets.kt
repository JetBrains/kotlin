/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

fun KotlinJsTargetDsl.calculateJsCompilerType(): KotlinJsCompilerType {
    return when {
        this is KotlinJsTarget && this.irTarget == null -> KotlinJsCompilerType.LEGACY
        this is KotlinJsIrTarget && !this.mixedMode -> KotlinJsCompilerType.IR
        this is KotlinJsTarget && this.irTarget != null -> KotlinJsCompilerType.BOTH
        else -> throw IllegalStateException("Unable to find previous Kotlin/JS compiler type for $this")
    }
}