/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.external.*
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinCompilationDescriptor.CompilationFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinTargetDescriptor.TargetFactory

class FakeCompilation(delegate: Delegate) : DecoratedExternalKotlinCompilation(delegate) {
    @Deprecated(
        "To configure compilation compiler options use 'compileTaskProvider':\ncompilation.compileTaskProvider.configure{\n" +
                "    compilerOptions {}\n}"
    )
    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    override val compilerOptions: HasCompilerOptions<KotlinJvmCompilerOptions>
        get() = super.compilerOptions as HasCompilerOptions<KotlinJvmCompilerOptions>
}

class FakeTarget(delegate: Delegate) : DecoratedExternalKotlinTarget(delegate),
    HasConfigurableCompilerOptions<KotlinJvmCompilerOptions> {

    @Suppress("UNCHECKED_CAST")
    override val compilations: NamedDomainObjectContainer<FakeCompilation>
        get() = super.compilations as NamedDomainObjectContainer<FakeCompilation>

    @ExperimentalKotlinGradlePluginApi
    override val compilerOptions: KotlinJvmCompilerOptions
        get() = super.compilerOptions as KotlinJvmCompilerOptions
}

fun ExternalKotlinTargetDescriptorBuilder<FakeTarget>.defaults() {
    targetName = "fake"
    platformType = KotlinPlatformType.jvm
    targetFactory = TargetFactory(::FakeTarget)
}

fun ExternalKotlinCompilationDescriptorBuilder<FakeCompilation>.defaults(
    kotlin: KotlinMultiplatformExtension,
    name: String = KotlinCompilation.MAIN_COMPILATION_NAME,
) {
    compilationName = name
    compilationFactory = CompilationFactory(::FakeCompilation)
    defaultSourceSet = kotlin.sourceSets.maybeCreate(name)
}
