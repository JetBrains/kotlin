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
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

class FakeCompilation(delegate: Delegate) : DecoratedExternalKotlinCompilation(delegate)
class FakeTarget(delegate: Delegate) : DecoratedExternalKotlinTarget(delegate)

fun ExternalKotlinTargetDescriptorBuilder<FakeTarget>.defaults() {
    targetName = "fake"
    platformType = KotlinPlatformType.jvm
    targetFactory = TargetFactory(::FakeTarget)
}

fun ExternalKotlinCompilationDescriptorBuilder<FakeCompilation>.defaults(
    kotlin: KotlinMultiplatformExtension,
    compilationName: String = KotlinCompilation.MAIN_COMPILATION_NAME
) {
    this.compilationName = compilationName
    compilationFactory = CompilationFactory(::FakeCompilation)
    defaultSourceSet = kotlin.sourceSets.maybeCreate(lowerCamelCaseName("fake", compilationName))
}
