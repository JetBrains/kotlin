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

package org.jetbrains.kotlin.gradle.plugin.experimental.internal

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.language.ComponentDependencies
import org.gradle.language.ComponentWithDependencies
import org.gradle.language.ComponentWithOutputs
import org.gradle.language.cpp.CppBinary
import org.gradle.language.cpp.internal.NativeVariantIdentity
import org.gradle.language.internal.DefaultComponentDependencies
import org.gradle.language.nativeplatform.internal.ComponentWithNames
import org.gradle.language.nativeplatform.internal.Names
import org.gradle.nativeplatform.OperatingSystemFamily
import org.gradle.nativeplatform.toolchain.NativeToolChain
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.experimental.ComponentWithBaseName
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeBinary
import org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets.KotlinNativeSourceSet
import org.jetbrains.kotlin.gradle.plugin.experimental.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget

/*
 *  We use the same configuration hierarchy as Gradle native:
 *
 *  componentImplementation (dependencies for the whole component ( = sourceSet): something like 'foo:bar:1.0')
 *    ^
 *    |
 *  binaryImplementation (dependnecies of a particular target/flavor) (= getDependencies.implementationDependencies)
 *    ^             ^                ^
 *    |             |                |
 *  linkLibraries  runtimeLibraries  klibs (dependencies by type: klib, static lib, shared lib etc)
 *
 */
abstract class AbstractKotlinNativeBinary(
        private val name: String,
        private val baseName: Provider<String>,
        override val component: AbstractKotlinNativeComponent,
        val variant: KotlinNativeVariant,
        override val kind: CompilerOutputKind,
        protected val objects: ObjectFactory,
        componentDependencies: KotlinNativeDependenciesImpl,
        configurations: ConfigurationContainer,
        val fileOperations: FileOperations
) : KotlinNativeBinary,
    ComponentWithNames,
    ComponentWithDependencies,
    ComponentWithBaseName,
    ComponentWithOutputs
{

    private val names = Names.of(name)
    override fun getNames(): Names = names

    override fun getName(): String = name

    final override val konanTarget: KonanTarget
        get() = variant.konanTarget

    val identity: NativeVariantIdentity = variant.identity

    override fun getTargetPlatform(): KotlinNativePlatform = variant.targetPlatform

    val sourceSet: KotlinNativeSourceSet
        get() = component.sources

    val buildType: KotlinNativeBuildType get() = variant.buildType
    open val debuggable: Boolean  get() = identity.isDebuggable
    open val optimized: Boolean   get() = identity.isOptimized

    override val sources: FileCollection
        get() = sourceSet.getAllSources(konanTarget)

    override val commonSources: FileCollection
        get() = sourceSet.getCommonMultiplatformSources() + sourceSet.getCommonNativeSources()

    private val dependencies = objects.newInstance<DefaultComponentDependencies>(
        DefaultComponentDependencies::class.java,
        name + "Implementation"
    ).apply {
        implementationDependencies.extendsFrom(componentDependencies.implementationDependencies)
    }

    override fun getDependencies(): ComponentDependencies = dependencies
    fun getImplementationDependencies(): Configuration = dependencies.implementationDependencies

    // A configuration containing klibs.
    override val klibs = configurations.create(names.withPrefix("klibs")).apply {
        isCanBeConsumed = false
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, KotlinUsages.KOTLIN_API))
        attributes.attribute(CppBinary.DEBUGGABLE_ATTRIBUTE, debuggable)
        attributes.attribute(CppBinary.OPTIMIZED_ATTRIBUTE, optimized)
        attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
        attributes.attribute(KotlinNativeBinary.KONAN_TARGET_ATTRIBUTE, konanTarget.name)
        attributes.attribute(KotlinNativeBinary.OLD_KONAN_TARGET_ATTRIBUTE, konanTarget.name)
        attributes.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, konanTarget.getGradleOSFamily(objects))
        extendsFrom(getImplementationDependencies())
    }

    override fun getBaseName(): Provider<String> = baseName

    override val compileTask: Property<KotlinNativeCompile> = objects.property(KotlinNativeCompile::class.java)

    open fun isDebuggable(): Boolean = debuggable
    open fun isOptimized(): Boolean = optimized

    // TODO: Support native libraries
    fun getLinkLibraries(): FileCollection = fileOperations.configurableFiles()
    fun getRuntimeLibraries(): FileCollection = fileOperations.configurableFiles()

    fun getToolChain(): NativeToolChain =
            throw NotImplementedError("Kotlin/Native doesn't support the Gradle's toolchain model.")

    /** A name of a root folder for this binary's output under the build directory. */
    internal abstract val outputRootName: String

    private val outputs: ConfigurableFileCollection = fileOperations.configurableFiles()
    override fun getOutputs() = outputs

    override val additionalCompilerOptions: Collection<String>
        get() = component.extraOpts

    override val linkerOpts: List<String>
        get() = component.target(konanTarget).linkerOpts
}