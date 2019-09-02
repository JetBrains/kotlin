/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.gradle.testing.native

import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.ide.konan.KotlinNativeRunConfigurationProvider
import org.jetbrains.kotlin.idea.run.AbstractKotlinMultiplatformTestMethodGradleConfigurationProducer
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.konan.isNative

class KotlinMultiplatformNativeTestMethodGradleConfigurationProducer
    : AbstractKotlinMultiplatformTestMethodGradleConfigurationProducer(), KotlinNativeRunConfigurationProvider
{
    override val isForTests: Boolean get() = true
    override fun isApplicable(module: Module, platform: TargetPlatform) = platform.isNative()
}