/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.interop

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.dependencies.NativeDependenciesPlugin

open class NativeInteropPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.apply<NativeDependenciesPlugin>()

        val interopStubGenerator = target.configurations.create(INTEROP_STUB_GENERATOR_CONFIGURATION)
        target.dependencies {
            interopStubGenerator(project(":kotlin-native:Interop:StubGenerator"))
            interopStubGenerator(project(":kotlin-native:endorsedLibraries:kotlinx.cli", "jvmRuntimeElements"))
        }

        target.extensions.add("kotlinNativeInterop", target.objects.domainObjectContainer(NamedNativeInteropConfig::class.java) {
            NamedNativeInteropConfig(target, it)
        })
    }

    companion object {
        const val INTEROP_STUB_GENERATOR_CONFIGURATION = "interopStubGenerator"
    }
}