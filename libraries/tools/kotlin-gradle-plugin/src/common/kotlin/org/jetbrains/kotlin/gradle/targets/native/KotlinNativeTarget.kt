/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.BasePlugin
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeBinaryContainer
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.metadata.*
import org.jetbrains.kotlin.gradle.targets.native.KotlinNativeBinaryTestRun
import org.jetbrains.kotlin.gradle.targets.native.KotlinNativeHostTestRun
import org.jetbrains.kotlin.gradle.targets.native.KotlinNativeSimulatorTestRun
import org.jetbrains.kotlin.gradle.targets.native.NativeBinaryTestRunSource
import org.jetbrains.kotlin.gradle.targets.native.internal.includeCommonizedCInteropMetadata
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

abstract class KotlinNativeTarget @Inject constructor(
    project: Project,
    val konanTarget: KonanTarget
) : KotlinTargetWithBinaries<KotlinNativeCompilation, KotlinNativeBinaryContainer>(
    project,
    KotlinPlatformType.native
) {

    init {
        attributes.attribute(konanTargetAttribute, konanTarget.name)
    }

    private val hostSpecificMetadataJarTaskName get() = disambiguateName("MetadataJar")

    internal val hostSpecificMetadataElementsConfigurationName get() = disambiguateName("MetadataElements")

    override val kotlinComponents: Set<KotlinTargetComponent> by lazy {
        if (!project.isKotlinGranularMetadataEnabled)
            return@lazy super.kotlinComponents

        val mainCompilation = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

        // NB: another usage context for the host-specific metadata may be added to this set below
        val mutableUsageContexts = createUsageContexts(mainCompilation).toMutableSet()

        project.whenEvaluated {
            val hostSpecificSourceSets = getHostSpecificSourceSets(project)
                .intersect(mainCompilation.allKotlinSourceSets)

            if (hostSpecificSourceSets.isNotEmpty()) {
                val hostSpecificMetadataJar = project.locateOrRegisterTask<Jar>(hostSpecificMetadataJarTaskName) { metadataJar ->
                    metadataJar.archiveAppendix.set(project.provider { disambiguationClassifier.orEmpty().toLowerCase() })
                    metadataJar.archiveClassifier.set("metadata")
                    metadataJar.group = BasePlugin.BUILD_GROUP
                    metadataJar.description = "Assembles Kotlin metadata of target '${name}'."

                    val publishable = this@KotlinNativeTarget.publishable
                    metadataJar.onlyIf { publishable }

                    val metadataCompilations = hostSpecificSourceSets.mapNotNull {
                        project.getMetadataCompilationForSourceSet(it)
                    }

                    metadataCompilations.forEach { compilation ->
                        metadataJar.from(project.filesWithUnpackedArchives(compilation.output.allOutputs, setOf("klib"))) { spec ->
                            spec.into(compilation.name)
                        }
                        metadataJar.dependsOn(compilation.output.classesDirs)

                        if (compilation is KotlinSharedNativeCompilation) {
                            project.includeCommonizedCInteropMetadata(metadataJar, compilation)
                        }
                    }
                }
                project.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, hostSpecificMetadataJar)

                val metadataConfiguration = project.configurations.getByName(hostSpecificMetadataElementsConfigurationName)
                project.artifacts.add(metadataConfiguration.name, hostSpecificMetadataJar) { artifact ->
                    artifact.classifier = "metadata"
                }

                mutableUsageContexts.add(
                    DefaultKotlinUsageContext(
                        mainCompilation,
                        project.usageByName(javaApiUsageForMavenScoping()),
                        metadataConfiguration.name,
                        includeIntoProjectStructureMetadata = false
                    )
                )
            }
        }

        val result = createKotlinVariant(targetName, mainCompilation, mutableUsageContexts)

        result.sourcesArtifacts = setOf(
            sourcesJarArtifact(mainCompilation, targetName, dashSeparatedName(targetName.toLowerCase()))
        )

        setOf(result)
    }

    override val binaries =
        // Use newInstance to allow accessing binaries by their names in Groovy using the extension mechanism.
        project.objects.newInstance(
            KotlinNativeBinaryContainer::class.java,
            this,
            project.objects.domainObjectSet(NativeBinary::class.java)
        )

    override val artifactsTaskName: String
        get() = disambiguateName("binaries")

    override val publishable: Boolean
        get() = konanTarget.enabledOnCurrentHost

    // User-visible constants
    val DEBUG = NativeBuildType.DEBUG
    val RELEASE = NativeBuildType.RELEASE

    val EXECUTABLE = NativeOutputKind.EXECUTABLE
    val FRAMEWORK = NativeOutputKind.FRAMEWORK
    val DYNAMIC = NativeOutputKind.DYNAMIC
    val STATIC = NativeOutputKind.STATIC

    companion object {
        val konanTargetAttribute = Attribute.of(
            "org.jetbrains.kotlin.native.target",
            String::class.java
        )
        val kotlinNativeBuildTypeAttribute = Attribute.of(
            "org.jetbrains.kotlin.native.build.type",
            String::class.java
        )
    }
}

private val hostManager by lazy { HostManager() }

private val targetsEnabledOnAllHosts by lazy { hostManager.enabledByHost.values.reduce { acc, targets -> acc intersect targets } }

/**
 * The set of konanTargets is considered 'host specific' if the shared compilation of said set can *not* be built
 * on *all* potential hosts. e.g. a set like (iosX64, macosX64) can only be built on macos hosts, and is therefore considered
 * 'host specific'.
 */
internal fun isHostSpecificKonanTargetsSet(konanTargets: Iterable<KonanTarget>): Boolean =
    konanTargets.none { target -> target in targetsEnabledOnAllHosts }

private fun <T> getHostSpecificElements(
    fragments: Iterable<T>,
    isNativeShared: (T) -> Boolean,
    getKonanTargets: (T) -> Set<KonanTarget>
): Set<T> = fragments.filterTo(mutableSetOf()) { isNativeShared(it) && isHostSpecificKonanTargetsSet(getKonanTargets(it)) }

internal fun getHostSpecificFragments(
    module: GradleKpmModule
): Set<GradleKpmFragment> = getHostSpecificElements<GradleKpmFragment>(
    module.fragments,
    isNativeShared = { it.isNativeShared() },
    getKonanTargets = {
        val nativeVariants = module.variantsContainingFragment(it).filterIsInstance<GradleKpmNativeVariantInternal>()
        nativeVariants.mapTo(mutableSetOf()) { it.konanTarget }
    }
)

internal fun getHostSpecificSourceSets(project: Project): Set<KotlinSourceSet> {
    return getHostSpecificElements(
        project.kotlinExtension.sourceSets,
        isNativeShared = { sourceSet -> isNativeSourceSet(sourceSet) },
        getKonanTargets = { sourceSet ->
            sourceSet.internal.compilations
                .filterIsInstance<KotlinNativeCompilation>()
                .mapTo(mutableSetOf()) { it.konanTarget }
        }
    )
}

/**
 * Returns all host-specific source sets that will be compiled to two or more targets
 */
internal fun getHostSpecificMainSharedSourceSets(project: Project): Set<KotlinSourceSet> {
    fun KotlinSourceSet.testOnly(): Boolean = internal.compilations.all { it.isTest() }

    fun KotlinSourceSet.isCompiledToSingleTarget(): Boolean {
        return internal
            .compilations
            // if for some reason [it.target] is not a [KotlinNativeTarget] then assume that it is not a host-specific source set
            .distinctBy { (it.target as? KotlinNativeTarget)?.konanTarget ?: return false }
            .size == 1
    }

    return getHostSpecificSourceSets(project)
        .filterNot { it.testOnly() }
        .filterNot { it.isCompiledToSingleTarget() }
        .toSet()
}


abstract class KotlinNativeTargetWithTests<T : KotlinNativeBinaryTestRun>(
    project: Project,
    konanTarget: KonanTarget
) : KotlinNativeTarget(project, konanTarget), KotlinTargetWithTests<NativeBinaryTestRunSource, T> {

    override lateinit var testRuns: NamedDomainObjectContainer<T>
        internal set
}

abstract class KotlinNativeTargetWithHostTests @Inject constructor(project: Project, konanTarget: KonanTarget) :
    KotlinNativeTargetWithTests<KotlinNativeHostTestRun>(project, konanTarget)

abstract class KotlinNativeTargetWithSimulatorTests @Inject constructor(project: Project, konanTarget: KonanTarget) :
    KotlinNativeTargetWithTests<KotlinNativeSimulatorTestRun>(project, konanTarget)
