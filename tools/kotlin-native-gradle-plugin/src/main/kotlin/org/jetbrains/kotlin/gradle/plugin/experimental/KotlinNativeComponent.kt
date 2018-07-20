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

import org.gradle.api.Action
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.publish.maven.MavenPom
import org.gradle.language.BinaryCollection
import org.gradle.language.ComponentWithBinaries
import org.gradle.language.ComponentWithDependencies
import org.gradle.nativeplatform.test.TestSuiteComponent
import org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets.KotlinNativeSourceSet
import org.jetbrains.kotlin.konan.target.KonanTarget

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

    /** Set native targets for this component. */
    fun target(vararg targets: String)

    /** Set additional compiler options for this component. */
    val extraOpts: Collection<String>

    fun extraOpts(vararg values: Any)
    fun extraOpts(values: List<Any>)

    fun pom(action: Action<MavenPom>)
    // endregion
}

/**
 * Class representing a test suite for Kotlin/Native
 */
interface KotlinNativeTestComponent : KotlinNativeComponent, TestSuiteComponent {
    val testedComponent: KotlinNativeComponent

    override fun getTestBinary(): Provider<KotlinNativeTestExecutable>
}