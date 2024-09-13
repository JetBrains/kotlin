/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.interop

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.PlatformManagerProvider
import org.jetbrains.kotlin.dependencies.NativeDependenciesExtension
import org.jetbrains.kotlin.gradle.plugin.konan.tasks.KonanJvmInteropTask
import org.jetbrains.kotlin.utils.capitalized

class NamedNativeInteropConfig(
        project: Project,
        private val _name: String,
) : Named {
    override fun getName(): String = _name

    val genTask = project.tasks.register<KonanJvmInteropTask>("gen${name.capitalized}InteropStubs") {
        dependsOn(project.extensions.getByType<NativeDependenciesExtension>().hostPlatformDependency)
        dependsOn(project.extensions.getByType<NativeDependenciesExtension>().llvmDependency)
        interopStubGeneratorClasspath.from(project.configurations.getByName(NativeInteropPlugin.INTEROP_STUB_GENERATOR_CONFIGURATION))
        interopStubGeneratorNativeLibs.from(project.configurations.getByName(NativeInteropPlugin.INTEROP_STUB_GENERATOR_CPP_CONFIGURATION))
        kotlinBridges.set(project.layout.buildDirectory.dir("nativeInteropStubs/${this@NamedNativeInteropConfig.name}/kotlin"))
        cBridge.set(project.layout.buildDirectory.file("nativeInteropStubs/${this@NamedNativeInteropConfig.name}/c/stubs.c"))
        temporaryFilesDir.set(project.layout.buildDirectory.dir("interopTemp"))
        platformManagerProvider.set(project.extensions.getByType<PlatformManagerProvider>())
    }
}