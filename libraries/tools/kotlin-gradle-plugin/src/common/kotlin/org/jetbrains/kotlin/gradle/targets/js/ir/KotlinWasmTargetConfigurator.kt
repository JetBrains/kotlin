/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinOnlyTargetConfigurator
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTargetConfigurator.Companion.configureJsLikeTarget

open class KotlinWasmTargetConfigurator :
    KotlinOnlyTargetConfigurator<KotlinJsIrCompilation, KotlinWasmTarget>(true) {

    override fun configureTarget(target: KotlinWasmTarget) {
        super.configureTarget(target)

        configureJsLikeTarget(target)
    }
}
