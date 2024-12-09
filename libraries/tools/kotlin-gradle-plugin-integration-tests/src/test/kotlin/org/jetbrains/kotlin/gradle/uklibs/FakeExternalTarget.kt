/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExternalKotlinTargetApi::class)

package org.jetbrains.kotlin.gradle.uklibs

import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.external.*

class FakeCompilation(delegate: Delegate) : DecoratedExternalKotlinCompilation(delegate) {
    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    override val compilerOptions: HasCompilerOptions<KotlinJvmCompilerOptions>
        get() = super.compilerOptions as HasCompilerOptions<KotlinJvmCompilerOptions>
}

class FakeTarget(delegate: Delegate) : DecoratedExternalKotlinTarget(delegate),
    HasConfigurableKotlinCompilerOptions<KotlinJvmCompilerOptions> {

    @Suppress("UNCHECKED_CAST")
    override val compilations: NamedDomainObjectContainer<FakeCompilation>
        get() = super.compilations as NamedDomainObjectContainer<FakeCompilation>

    override val compilerOptions: KotlinJvmCompilerOptions
        get() = super.compilerOptions as KotlinJvmCompilerOptions
}

fun ExternalKotlinTargetDescriptorBuilder<FakeTarget>.defaults() {
    targetName = "fake"
    platformType = KotlinPlatformType.jvm
    targetFactory = ExternalKotlinTargetDescriptor.TargetFactory(::FakeTarget)
}

fun ExternalKotlinCompilationDescriptorBuilder<FakeCompilation>.defaults(
    kotlin: KotlinMultiplatformExtension,
    name: String = KotlinCompilation.MAIN_COMPILATION_NAME,
) {
    compilationName = name
    compilationFactory = ExternalKotlinCompilationDescriptor.CompilationFactory(::FakeCompilation)
    defaultSourceSet = kotlin.sourceSets.maybeCreate(name)
}