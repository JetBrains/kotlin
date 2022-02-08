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
import org.gradle.jvm.tasks.Jar
import org.gradle.util.WrapUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeBinaryContainer
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.isNativeShared
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.variantsContainingFragment
import org.jetbrains.kotlin.gradle.targets.metadata.*
import org.jetbrains.kotlin.gradle.targets.metadata.filesWithUnpackedArchives
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
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

open class KotlinNativeTarget @Inject constructor(
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
        project.objects.newInstance(KotlinNativeBinaryContainer::class.java, this, WrapUtil.toDomainObjectSet(NativeBinary::class.java))

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

internal fun isHostSpecificKonanTargetsSet(konanTargets: Iterable<KonanTarget>): Boolean {
    val enabledByHost = hostManager.enabledByHost
    val allHosts = enabledByHost.keys
    fun canBeBuiltOnHosts(konanTarget: KonanTarget) = enabledByHost.filterValues { konanTarget in it }.keys
    return konanTargets.flatMapTo(mutableSetOf(), ::canBeBuiltOnHosts) != allHosts
}

private fun <T> getHostSpecificElements(
    fragments: Iterable<T>,
    isNativeShared: (T) -> Boolean,
    getKonanTargets: (T) -> Set<KonanTarget>
): Set<T> = fragments.filterTo(mutableSetOf()) { isNativeShared(it) && isHostSpecificKonanTargetsSet(getKonanTargets(it)) }

internal fun getHostSpecificFragments(
    module: KotlinGradleModule
): Set<KotlinGradleFragment> = getHostSpecificElements<KotlinGradleFragment>(
    module.fragments,
    isNativeShared = { it.isNativeShared() },
    getKonanTargets = {
        val nativeVariants = module.variantsContainingFragment(it).filterIsInstance<KotlinNativeVariantInternal>()
        nativeVariants.mapTo(mutableSetOf()) { it.konanTarget }
    }
)

internal fun getHostSpecificSourceSets(project: Project): Set<KotlinSourceSet> {
    val compilationsBySourceSet = CompilationSourceSetUtil.compilationsBySourceSets(project).mapValues { (_, compilations) ->
        compilations.filter { it !is KotlinMetadataCompilation<*> }
    }

    return getHostSpecificElements(
        project.kotlinExtension.sourceSets,
        isNativeShared = { sourceSet ->
            val compilations = compilationsBySourceSet[sourceSet].orEmpty()
            compilations.isNotEmpty() && compilations.all { it.platformType == KotlinPlatformType.native }
        },
        getKonanTargets = { sourceSet ->
            compilationsBySourceSet[sourceSet].orEmpty()
                .filterIsInstance<KotlinNativeCompilation>()
                .mapTo(mutableSetOf()) { it.konanTarget }
        }
    )
}

abstract class KotlinNativeTargetWithTests<T : KotlinNativeBinaryTestRun>(
    project: Project,
    konanTarget: KonanTarget
) : KotlinNativeTarget(project, konanTarget), KotlinTargetWithTests<NativeBinaryTestRunSource, T> {

    override lateinit var testRuns: NamedDomainObjectContainer<T>
        internal set
}

open class KotlinNativeTargetWithHostTests @Inject constructor(project: Project, konanTarget: KonanTarget) :
    KotlinNativeTargetWithTests<KotlinNativeHostTestRun>(project, konanTarget)

open class KotlinNativeTargetWithSimulatorTests @Inject constructor(project: Project, konanTarget: KonanTarget) :
    KotlinNativeTargetWithTests<KotlinNativeSimulatorTestRun>(project, konanTarget)
