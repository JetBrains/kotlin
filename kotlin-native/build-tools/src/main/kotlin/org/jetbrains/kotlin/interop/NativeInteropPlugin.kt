/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.interop

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.LibraryElements
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.from
import org.jetbrains.kotlin.cpp.CppUsage
import org.jetbrains.kotlin.dependencies.NativeDependenciesExtension
import org.jetbrains.kotlin.dependencies.NativeDependenciesPlugin
import org.jetbrains.kotlin.gradle.plugin.konan.tasks.KonanJvmInteropTask
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import org.jetbrains.kotlin.utils.capitalized
import kotlin.text.set

open class NativeInteropPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.apply<NativeDependenciesPlugin>()

        target.extensions.create<NativeInteropExtension>("nativeInteropPlugin")

        val interopStubGenerator = target.configurations.create(INTEROP_STUB_GENERATOR_CONFIGURATION)
        val interopStubGeneratorCppRuntime = target.configurations.create(INTEROP_STUB_GENERATOR_CPP_RUNTIME_CONFIGURATION) {
            isCanBeConsumed = false
            isCanBeResolved = true
            attributes {
                attribute(CppUsage.USAGE_ATTRIBUTE, target.objects.named(CppUsage.LIBRARY_RUNTIME))
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, target.objects.named(LibraryElements.DYNAMIC_LIB))
                attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
            }
        }

        target.dependencies {
            interopStubGenerator(project(":kotlin-native:Interop:StubGenerator"))
            interopStubGenerator(project(":kotlin-native:endorsedLibraries:kotlinx.cli", "jvmRuntimeElements"))
            interopStubGeneratorCppRuntime(project(":kotlin-native:libclangInterop"))
            interopStubGeneratorCppRuntime(project(":kotlin-native:Interop:Runtime"))
        }

        target.tasks.register<KonanJvmInteropTask>("genInteropStubs") {
            dependsOn(target.extensions.getByType<NativeDependenciesExtension>().hostPlatformDependency)
            dependsOn(target.extensions.getByType<NativeDependenciesExtension>().llvmDependency)
            interopStubGeneratorClasspath.from(interopStubGenerator)
            interopStubGeneratorNativeLibraries.from(interopStubGeneratorCppRuntime)
            outputDirectory.set(project.layout.buildDirectory.dir("nativeInteropStubs"))
        }
    }

    companion object {
        const val INTEROP_STUB_GENERATOR_CONFIGURATION = "interopStubGenerator"
        const val INTEROP_STUB_GENERATOR_CPP_RUNTIME_CONFIGURATION = "interopStubGeneratorCppRuntime"
    }
}