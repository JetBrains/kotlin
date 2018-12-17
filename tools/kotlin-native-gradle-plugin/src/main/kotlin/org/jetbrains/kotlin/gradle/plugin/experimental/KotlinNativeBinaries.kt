/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin.experimental

import org.gradle.api.attributes.Attribute
import org.gradle.api.component.BuildableComponent
import org.gradle.api.component.PublishableComponent
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.language.ComponentWithDependencies
import org.gradle.language.ComponentWithOutputs
import org.gradle.language.nativeplatform.internal.ConfigurableComponentWithLinkUsage
import org.gradle.language.nativeplatform.internal.ConfigurableComponentWithRuntimeUsage
import org.gradle.nativeplatform.test.TestComponent
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.BitcodeEmbeddingMode
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.KotlinNativePlatform
import org.jetbrains.kotlin.gradle.plugin.experimental.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget

// TODO: implement ComponentWithObjectFiles when we build klibs as objects
interface KotlinNativeBinary : ComponentWithDependencies, BuildableComponent, ComponentWithOutputs {

    /** A component this binary belongs to. */
    val component: KotlinNativeComponent

    /** Returns the source files of this binary. */
    val sources: FileCollection

    /**
     * Returns common sources used to build this binary
     * (both common for all native targets and avalable via expectedBy relation).
     */
    val commonSources: FileCollection

    /**
     * Konan target the library is built for
     */
    val konanTarget: KonanTarget

    /**
     * Gradle NativePlatform object the binary is built for.
     */
    fun getTargetPlatform(): KotlinNativePlatform

    /** Compile task for this library */
    val compileTask: Provider<KotlinNativeCompile>

    // TODO: Support native link and runtime libraries here.
    // Looks like we need at least 3 file collections here: for klibs, for linktime native libraries and for runtime native libraries.
    /**
     * The link libraries (klibs only!) used to link this binary.
     * Includes the link libraries of the component's dependencies.
     */
    val klibs: FileCollection

    /**
     * Binary kind in terms of the KN compiler (program, library, dynamic etc).
     */
    val kind: CompilerOutputKind

    /**
     * Additional command line options passed to the compiler when this binary is compiled.
     */
    val additionalCompilerOptions: Collection<String>

    /**
     * Additional options passed to a linker when this binary is built.
     */
    val linkerOpts: List<String>

    companion object {
        val KONAN_TARGET_ATTRIBUTE = Attribute.of("org.jetbrains.kotlin.native.target", String::class.java)
        val OLD_KONAN_TARGET_ATTRIBUTE = Attribute.of("org.gradle.native.kotlin.platform",  String::class.java)
    }
}

/**
 * Represents Kotlin/Native executable.
 */
// TODO: Consider implementing ComponentWithExecutable and ComponentWithInstallation.
interface KotlinNativeExecutable : KotlinNativeBinary,
        PublishableComponent,
        ConfigurableComponentWithRuntimeUsage

/**
 *  A component representing a klibrary.
 */
// TODO: Consider implementing ComponentWithLinkFile.
interface KotlinNativeLibrary : KotlinNativeBinary,
        PublishableComponent,
        ConfigurableComponentWithLinkUsage

/**
 * Represents an Objective C framework compiled from Kotlin/Native sources.
 */
interface KotlinNativeFramework : KotlinNativeBinary {
    /** Klibs exported in the framework. */
    val export: FileCollection

    /** Mode of bitcode embedding: disabled, enabled or marker. */
    var embedBitcode: BitcodeEmbeddingMode
}

/**
 * A shared library compiled from Kotlin/Native sources.
 */
interface KotlinNativeDynamic : KotlinNativeBinary

/**
 * A static library compiled from Kotlin/Native sources.
 */
interface KotlinNativeStatic : KotlinNativeBinary

/**
 * Represents a test executable.
 */
// TODO: Consider implementing ComponentWithExecutable and ComponentWithInstallation.
interface KotlinNativeTestExecutable : KotlinNativeBinary, TestComponent