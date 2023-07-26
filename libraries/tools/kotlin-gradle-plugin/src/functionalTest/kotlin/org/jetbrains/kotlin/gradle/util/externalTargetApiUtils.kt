/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.external.DecoratedExternalKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.external.DecoratedExternalKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinCompilationDescriptor.CompilationFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinCompilationDescriptorBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinTargetDescriptor.TargetFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinTargetDescriptorBuilder

class FakeCompilation(delegate: Delegate) : DecoratedExternalKotlinCompilation(delegate)
class FakeTarget(delegate: Delegate) : DecoratedExternalKotlinTarget(delegate)

fun ExternalKotlinTargetDescriptorBuilder<FakeTarget>.defaults() {
    targetName = "fake"
    platformType = KotlinPlatformType.jvm
    targetFactory = TargetFactory(::FakeTarget)
}

fun ExternalKotlinCompilationDescriptorBuilder<FakeCompilation>.defaults(
    kotlin: KotlinMultiplatformExtension,
    name: String = KotlinCompilation.MAIN_COMPILATION_NAME
) {
    compilationName = name
    compilationFactory = CompilationFactory(::FakeCompilation)
    defaultSourceSet = kotlin.sourceSets.maybeCreate(name)
}
