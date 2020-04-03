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
import org.gradle.api.attributes.Usage
import org.gradle.jvm.tasks.Jar
import org.gradle.util.WrapUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeBinaryContainer
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.targets.metadata.filesWithUnpackedArchives
import org.jetbrains.kotlin.gradle.targets.metadata.getPublishedCommonSourceSets
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.targets.native.KotlinNativeBinaryTestRun
import org.jetbrains.kotlin.gradle.targets.native.KotlinNativeHostTestRun
import org.jetbrains.kotlin.gradle.targets.native.KotlinNativeSimulatorTestRun
import org.jetbrains.kotlin.gradle.targets.native.NativeBinaryTestRunSource
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.setArchiveAppendixCompatible
import org.jetbrains.kotlin.gradle.utils.setArchiveClassifierCompatible
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

    private val hostSpecificMetadataElementsConfigurationName get() = disambiguateName("MetadataElements")

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
                val hostSpecificMetadataJar = project.locateOrRegisterTask<Jar>(hostSpecificMetadataJarTaskName) {
                    it.setArchiveAppendixCompatible { disambiguationClassifier.orEmpty().toLowerCase() }
                    it.setArchiveClassifierCompatible { "metadata" }
                }
                project.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, hostSpecificMetadataJar)

                hostSpecificMetadataJar.configure { metadataJar ->
                    metadataJar.onlyIf { this@KotlinNativeTarget.publishable }

                    val metadataCompilations = hostSpecificSourceSets.mapNotNull {
                        project.multiplatformExtension.metadata().compilations.findByName(it.name)
                    }

                    metadataCompilations.forEach {
                        metadataJar.from(project.filesWithUnpackedArchives(it.output.allOutputs, setOf("klib"))) { spec ->
                            spec.into(it.name)
                        }
                        metadataJar.dependsOn(it.output.classesDirs)
                    }
                }

                val metadataConfiguration = project.configurations.findByName(hostSpecificMetadataElementsConfigurationName)
                    ?: project.configurations.create(hostSpecificMetadataElementsConfigurationName) { configuration ->
                        configuration.isCanBeConsumed = false
                        configuration.isCanBeResolved = false
                        project.artifacts.add(configuration.name, hostSpecificMetadataJar) { artifact ->
                            artifact.classifier = "metadata"
                        }
                    }

                val metadataAttributes =
                    HierarchyAttributeContainer(project.configurations.getByName(apiElementsConfigurationName).attributes).apply {
                        attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
                    }

                mutableUsageContexts.add(
                    DefaultKotlinUsageContext(
                        mainCompilation,
                        project.usageByName(javaApiUsageForMavenScoping()),
                        metadataConfiguration.name,
                        overrideConfigurationAttributes = metadataAttributes,
                        includeIntoProjectStructureMetadata = false
                    )
                )
            }
        }

        setOf(createKotlinVariant(targetName, mainCompilation, mutableUsageContexts))
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
    }
}

internal fun getHostSpecificSourceSets(project: Project): List<KotlinSourceSet> {
    val compilationsBySourceSet = CompilationSourceSetUtil.compilationsBySourceSets(project)

    val enabledByHost = HostManager().enabledByHost
    val allHosts = enabledByHost.keys

    fun canBeBuiltOnHosts(konanTarget: KonanTarget) = enabledByHost.filterValues { konanTarget in it }.keys

    return project.kotlinExtension.sourceSets.filter { sourceSet ->
        // If the source set participates in compilations such that some host can't run either of them, then on that host,
        // we can't analyze the source set, so the source set's metadata can't be published from that host, and therefore
        // we consider it platform-specific and publish as a part of the Native targets where the source set takes part,
        // not the common metadata artifact;
        val platformCompilations = compilationsBySourceSet.getValue(sourceSet).filter { it !is KotlinMetadataCompilation }
        val nativeCompilations = platformCompilations.filterIsInstance<KotlinNativeCompilation>()
        val nativeEnabledOn = nativeCompilations.flatMapTo(mutableSetOf()) { compilation -> canBeBuiltOnHosts(compilation.konanTarget) }
        platformCompilations == nativeCompilations && allHosts != nativeEnabledOn
    }
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