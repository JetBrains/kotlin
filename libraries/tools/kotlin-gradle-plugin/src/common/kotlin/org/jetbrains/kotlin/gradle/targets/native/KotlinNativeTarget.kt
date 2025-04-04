/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.publication.setUpResourcesVariant
import org.jetbrains.kotlin.gradle.plugin.sources.awaitPlatformCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.metadata.isNativeSourceSet
import org.jetbrains.kotlin.gradle.targets.native.*
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.gradle.utils.klibModuleName
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.addIfNotNull
import javax.inject.Inject

abstract class KotlinNativeTarget @Inject constructor(
    project: Project,
    val konanTarget: KonanTarget,
) : HasConfigurableKotlinCompilerOptions<KotlinNativeCompilerOptions>,
    KotlinTargetWithBinaries<KotlinNativeCompilation, KotlinNativeBinaryContainer>(
        project,
        KotlinPlatformType.native
    ) {

    init {
        attributes.attribute(konanTargetAttribute, konanTarget.name)
    }

    internal val hostSpecificMetadataElementsConfigurationName get() = disambiguateName("MetadataElements")

    override val kotlinComponents: Set<KotlinTargetComponent> by lazy {

        val mainCompilation = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

        // NB: another usage context for the host-specific metadata may be added to this set below
        val mutableUsageContexts = createUsageContexts(mainCompilation).toMutableSet()

        project.launchInStage(KotlinPluginLifecycle.Stage.AfterFinaliseDsl) {
            val hostSpecificSourceSets = getHostSpecificSourceSets(project)
                .intersect(mainCompilation.allKotlinSourceSets)

            if (hostSpecificSourceSets.isNotEmpty()) {
                mutableUsageContexts.add(
                    DefaultKotlinUsageContext(
                        mainCompilation,
                        KotlinUsageContext.MavenScope.COMPILE,
                        hostSpecificMetadataElementsConfigurationName,
                        includeIntoProjectStructureMetadata = false
                    )
                )
            }
        }

        mutableUsageContexts.addIfNotNull(
            createSourcesJarAndUsageContextIfPublishable(
                mainCompilation,
                targetName,
                dashSeparatedName(targetName.toLowerCaseAsciiOnly())
            )
        )

        mutableUsageContexts.addIfNotNull(
            setUpResourcesVariant(
                mainCompilation
            )
        )

        val result = createKotlinVariant(targetName, mainCompilation, mutableUsageContexts)

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
        get() = enabledOnCurrentHostForKlibCompilation

    override val compilerOptions: KotlinNativeCompilerOptions = project.objects
        .newInstance<KotlinNativeCompilerOptionsDefault>()
        .apply {
            moduleName.convention(
                project.klibModuleName(
                    project.baseModuleName()
                )
            )
        }

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
        val kotlinNativeFrameworkNameAttribute = Attribute.of(
            "org.jetbrains.kotlin.native.framework.name",
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

private suspend fun <T> getHostSpecificElements(
    fragments: Iterable<T>,
    isNativeShared: suspend (T) -> Boolean,
    getKonanTargets: suspend (T) -> Set<KonanTarget>,
): Set<T> = fragments.filterTo(mutableSetOf()) { isNativeShared(it) && isHostSpecificKonanTargetsSet(getKonanTargets(it)) }

internal suspend fun getHostSpecificSourceSets(project: Project): Set<KotlinSourceSet> {
    return getHostSpecificElements(
        project.kotlinExtension.awaitSourceSets(),
        isNativeShared = { sourceSet -> sourceSet.isNativeSourceSet.await() },
        getKonanTargets = { sourceSet ->
            sourceSet.internal.awaitPlatformCompilations()
                .filterIsInstance<KotlinNativeCompilation>()
                .mapTo(mutableSetOf()) { it.konanTarget }
        }
    )
}

/**
 * Returns all host-specific source sets that will be compiled to two or more targets
 */
internal suspend fun getHostSpecificMainSharedSourceSets(project: Project): Set<KotlinSourceSet> {
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
    konanTarget: KonanTarget,
) : KotlinNativeTarget(project, konanTarget), KotlinTargetWithTests<NativeBinaryTestRunSource, T>

abstract class KotlinNativeTargetWithHostTests @Inject constructor(project: Project, konanTarget: KonanTarget) :
    KotlinNativeTargetWithTests<KotlinNativeHostTestRun>(project, konanTarget) {
    override val testRuns: NamedDomainObjectContainer<KotlinNativeHostTestRun> by lazy {
        project.container(KotlinNativeHostTestRun::class.java, KotlinNativeHostTestRunFactory(this))
    }
}

abstract class KotlinNativeTargetWithSimulatorTests @Inject constructor(project: Project, konanTarget: KonanTarget) :
    KotlinNativeTargetWithTests<KotlinNativeSimulatorTestRun>(project, konanTarget) {
    override val testRuns: NamedDomainObjectContainer<KotlinNativeSimulatorTestRun> by lazy {
        project.container(KotlinNativeSimulatorTestRun::class.java, KotlinNativeSimulatorTestRunFactory(this))
    }
}
