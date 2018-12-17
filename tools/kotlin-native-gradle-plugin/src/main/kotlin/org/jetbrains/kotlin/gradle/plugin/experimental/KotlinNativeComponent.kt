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

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.publish.maven.MavenPom
import org.gradle.language.BinaryCollection
import org.gradle.language.ComponentDependencies
import org.gradle.language.ComponentWithBinaries
import org.gradle.language.ComponentWithDependencies
import org.gradle.nativeplatform.test.TestSuiteComponent
import org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets.KotlinNativeSourceSet
import org.jetbrains.kotlin.konan.target.KonanTarget

interface KotlinNativeDependencies: ComponentDependencies {
    val cinterops : NamedDomainObjectContainer<out CInterop>
    fun cinterop(name: String): CInterop
    fun cinterop(name: String, action: CInterop.() -> Unit)
    fun cinterop(name: String, action: Closure<Unit>)
    fun cinterop(name: String, action: Action<CInterop>)

    fun export(notation: Any)
    fun export(notation: Any, configure: Closure<*>)
    fun export(notation: Any, configure: Action<in Dependency>)

    var transitiveExport: Boolean
}

interface TargetSettings {
    /**
     * Additional options passed to a linker when a binary is built.
     */
    val linkerOpts: MutableList<String>
    fun linkerOpts(values: List<String>)
    fun linkerOpts(vararg values: String)
}

/**
 *  Class representing a Kotlin/Native component: application or library (both klib and dynamic)
 *  built for different targets.
 */
interface KotlinNativeComponent: ComponentWithBinaries, ComponentWithDependencies {

    /**
     * Defines the source files or directories of this component. You can add files or directories to this collection.
     * When a directory is added, all source files are included for compilation. When this collection is empty,
     * the directory src/main/kotlin is used by default.
     */
    val sources: KotlinNativeSourceSet

    /** Specifies Kotlin/Native targets used to build this component. */
    val konanTargets: SetProperty<KonanTarget>

    /** Returns the binaries for this library. */
    override fun getBinaries(): BinaryCollection<out KotlinNativeBinary>

    /** Returns the implementation dependencies of this component. */
    fun getImplementationDependencies(): Configuration

    // region DSL

    /** Allows a user to specify targets this component is built for. */
    var targets: List<String>

    /** Provides an access to component's dependencies including cinterop and jsinterop DSL */
    override fun getDependencies(): KotlinNativeDependencies

    fun dependencies(action: KotlinNativeDependencies.() -> Unit)
    fun dependencies(action: Closure<Unit>)
    fun dependencies(action: Action<KotlinNativeDependencies>)

    /** Set native targets for this component. */
    @Deprecated("Use the 'targets' property instead. E.g. targets = ['macos_x64', 'linux_x64']")
    fun target(vararg targets: String)

    /** Allows providing target-specific compiler options. */
    fun target(target: String): TargetSettings
    fun target(konanTarget: KonanTarget): TargetSettings
    fun target(target: String, action: TargetSettings.() -> Unit)
    fun target(target: String, action: Closure<Unit>)
    fun target(target: String, action: Action<TargetSettings>)
    fun allTargets(action: TargetSettings.() -> Unit)
    fun allTargets(action: Closure<Unit>)
    fun allTargets(action: Action<TargetSettings>)

    /** Set additional compiler options for this component. */
    val extraOpts: Collection<String>

    fun extraOpts(vararg values: Any)
    fun extraOpts(values: List<Any>)

    fun pom(action: Action<MavenPom>)

    val publishJavadoc: Boolean
    val publishSources: Boolean

    /** Allows setting custom entry point for executables */
    var entryPoint: String?
    fun entryPoint(value: String)
    // endregion
}

/**
 * Class representing a test suite for Kotlin/Native
 */
interface KotlinNativeTestComponent : KotlinNativeComponent, TestSuiteComponent {
    val testedComponent: KotlinNativeComponent

    override fun getTestBinary(): Provider<KotlinNativeTestExecutable>
}