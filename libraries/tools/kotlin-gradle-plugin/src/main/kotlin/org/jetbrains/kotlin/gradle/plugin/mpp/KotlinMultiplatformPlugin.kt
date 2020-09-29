/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.internal.FeaturePreviews
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.util.ConfigureUtil
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.configureOrCreate
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.customizeKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin.Companion.sourceSetFreeCompilerArgsPropertyName
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.checkSourceSetVisibilityRequirements
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTargetPreset
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.statistics.metrics.StringMetrics

class KotlinMultiplatformPlugin(
    private val kotlinPluginVersion: String,
    private val featurePreviews: FeaturePreviews // TODO get rid of this internal API usage once we don't need it
) : Plugin<Project> {

    private class TargetFromPresetExtension(val targetsContainer: KotlinTargetsContainerWithPresets) {
        fun <T : KotlinTarget> fromPreset(preset: KotlinTargetPreset<T>, name: String, configureClosure: Closure<*>): T =
            fromPreset(preset, name) { ConfigureUtil.configure(configureClosure, this) }

        @JvmOverloads
        fun <T : KotlinTarget> fromPreset(preset: KotlinTargetPreset<T>, name: String, configureAction: T.() -> Unit = { }): T =
            targetsContainer.configureOrCreate(name, preset, configureAction)
    }

    override fun apply(project: Project) {
        checkGradleCompatibility("the Kotlin Multiplatform plugin", GradleVersion.version("6.0"))

        project.plugins.apply(JavaBasePlugin::class.java)

        if (PropertiesProvider(project).mppStabilityNoWarn != true) {
            SingleWarningPerBuild.show(
                project,
                "Kotlin Multiplatform Projects are an Alpha feature. " +
                        "See: https://kotlinlang.org/docs/reference/evolution/components-stability.html. " +
                        "To hide this message, add '$STABILITY_NOWARN_FLAG=true' to the Gradle properties.\n"
            )
        }

        val targetsContainer = project.container(KotlinTarget::class.java)
        val kotlinMultiplatformExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        val targetsFromPreset = TargetFromPresetExtension(kotlinMultiplatformExtension)

        kotlinMultiplatformExtension.apply {
            DslObject(targetsContainer).addConvention("fromPreset", targetsFromPreset)

            targets = targetsContainer
            addExtension("targets", targets)

            presets = project.container(KotlinTargetPreset::class.java)
            addExtension("presets", presets)

            defaultJsCompilerType = PropertiesProvider(project).jsCompiler
        }

        setupDefaultPresets(project)
        customizeKotlinDependencies(project)
        configureSourceSets(project)

        // set up metadata publishing
        targetsFromPreset.fromPreset(
            KotlinMetadataTargetPreset(project, kotlinPluginVersion),
            METADATA_TARGET_NAME
        )
        configurePublishingWithMavenPublish(project)

        targetsContainer.withType(AbstractKotlinTarget::class.java).all { applyUserDefinedAttributes(it) }

        // propagate compiler plugin options to the source set language settings
        setupAdditionalCompilerArguments(project)
        project.setupGeneralKotlinExtensionParameters()

        project.pluginManager.apply(ScriptingGradleSubplugin::class.java)

        exportProjectStructureMetadataForOtherBuilds(project)
    }

    private fun exportProjectStructureMetadataForOtherBuilds(
        project: Project
    ) {
        GlobalProjectStructureMetadataStorage.registerProjectStructureMetadata(project) {
            checkNotNull(buildKotlinProjectStructureMetadata(project))
        }
    }

    private fun setupAdditionalCompilerArguments(project: Project) {
        // common source sets use the compiler options from the metadata compilation:
        val metadataCompilation =
            project.multiplatformExtension.metadata().compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

        val primaryCompilationsBySourceSet by lazy { // don't evaluate eagerly: Android targets are not created at this point
            val allCompilationsForSourceSets = CompilationSourceSetUtil.compilationsBySourceSets(project).mapValues { (_, compilations) ->
                compilations.filter { it.target.platformType != KotlinPlatformType.common }
            }

            allCompilationsForSourceSets.mapValues { (_, compilations) -> // choose one primary compilation
                when (compilations.size) {
                    0 -> metadataCompilation
                    1 -> compilations.single()
                    else -> {
                        val sourceSetTargets = compilations.map { it.target }.distinct()
                        when (sourceSetTargets.size) {
                            1 -> sourceSetTargets.single().compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                                ?: // use any of the compilations for now, looks OK for Android TODO maybe reconsider
                                compilations.first()
                            else -> metadataCompilation
                        }
                    }
                }
            }
        }

        project.kotlinExtension.sourceSets.all { sourceSet ->
            (sourceSet.languageSettings as? DefaultLanguageSettingsBuilder)?.run {
                compilerPluginOptionsTask = lazy {
                    val associatedCompilation = primaryCompilationsBySourceSet[sourceSet] ?: metadataCompilation
                    project.tasks.getByName(associatedCompilation.compileKotlinTaskName) as AbstractCompile
                }
            }
        }
    }

    fun setupDefaultPresets(project: Project) {
        with(project.multiplatformExtension.presets) {
            add(KotlinJvmTargetPreset(project, kotlinPluginVersion))
            add(KotlinJsTargetPreset(project, kotlinPluginVersion).apply { irPreset = null })
            add(KotlinJsIrTargetPreset(project, kotlinPluginVersion).apply { mixedMode = false })
            add(
                KotlinJsTargetPreset(
                    project,
                    kotlinPluginVersion
                ).apply {
                    irPreset = KotlinJsIrTargetPreset(project, kotlinPluginVersion)
                        .apply { mixedMode = true }
                }
            )
            add(KotlinAndroidTargetPreset(project, kotlinPluginVersion))
            add(KotlinJvmWithJavaTargetPreset(project, kotlinPluginVersion))

            // Note: modifying these sets should also be reflected in the DSL code generator, see 'presetEntries.kt'
            val nativeTargetsWithHostTests = setOf(LINUX_X64, MACOS_X64, MINGW_X64)
            val nativeTargetsWithSimulatorTests = setOf(IOS_X64, WATCHOS_X86, TVOS_X64)
            val disabledNativeTargets = setOf(WATCHOS_X64)

            HostManager().targets
                .filter { (_, konanTarget) -> konanTarget !in disabledNativeTargets }
                .forEach { (_, konanTarget) ->
                    val targetToAdd = when (konanTarget) {
                        in nativeTargetsWithHostTests ->
                            KotlinNativeTargetWithHostTestsPreset(konanTarget.presetName, project, konanTarget, kotlinPluginVersion)
                        in nativeTargetsWithSimulatorTests ->
                            KotlinNativeTargetWithSimulatorTestsPreset(konanTarget.presetName, project, konanTarget, kotlinPluginVersion)
                        else -> KotlinNativeTargetPreset(konanTarget.presetName, project, konanTarget, kotlinPluginVersion)
                    }

                    add(targetToAdd)
                }
        }
    }

    private fun configurePublishingWithMavenPublish(project: Project) = project.pluginManager.withPlugin("maven-publish") { _ ->

        val targets = project.multiplatformExtension.targets
        val kotlinSoftwareComponent = project.multiplatformExtension.rootSoftwareComponent

        project.extensions.configure(PublishingExtension::class.java) { publishing ->

            // The root publication that references the platform specific publications as its variants:
            publishing.publications.create("kotlinMultiplatform", MavenPublication::class.java).apply {
                from(kotlinSoftwareComponent)
                (this as MavenPublicationInternal).publishWithOriginalFileName()
                kotlinSoftwareComponent.publicationDelegate = this@apply

                project.multiplatformExtension.metadata {}.kotlinComponents.filterIsInstance<KotlinTargetComponentWithPublication>()
                    .single().publicationDelegate = this@apply
            }

            // Enforce the order of creating the publications, since the metadata publication is used in the other publications:
            (targets.getByName(METADATA_TARGET_NAME) as AbstractKotlinTarget).createMavenPublications(publishing.publications)
            targets
                .withType(AbstractKotlinTarget::class.java).matching { it.publishable && it.name != METADATA_TARGET_NAME }
                .all {
                    if (it is KotlinAndroidTarget || it is KotlinMetadataTarget)
                    // Android targets have their variants created in afterEvaluate; TODO handle this better?
                    // Kotlin Metadata targets rely on complete source sets hierearchy and cannot be inspected for publication earlier
                        project.whenEvaluated { it.createMavenPublications(publishing.publications) }
                    else
                        it.createMavenPublications(publishing.publications)
                }
        }

        project.components.add(kotlinSoftwareComponent)
    }

    private fun AbstractKotlinTarget.createMavenPublications(publications: PublicationContainer) {
        components
            .map { gradleComponent -> gradleComponent to kotlinComponents.single { it.name == gradleComponent.name } }
            .filter { (_, kotlinComponent) -> kotlinComponent.publishable }
            .forEach { (gradleComponent, kotlinComponent) ->
                val componentPublication = publications.create(kotlinComponent.name, MavenPublication::class.java).apply {
                    // do this in whenEvaluated since older Gradle versions seem to check the files in the variant eagerly:
                    project.whenEvaluated {
                        from(gradleComponent)
                        kotlinComponent.sourcesArtifacts.forEach { sourceArtifact ->
                            artifact(sourceArtifact)
                        }
                    }
                    (this as MavenPublicationInternal).publishWithOriginalFileName()
                    artifactId = kotlinComponent.defaultArtifactId

                    pom.withXml { xml ->
                        if (PropertiesProvider(project).keepMppDependenciesIntactInPoms != true)
                            project.rewritePomMppDependenciesToActualTargetModules(xml, kotlinComponent) { id ->
                                filterMetadataDependencies(this@createMavenPublications, id)
                            }
                    }
                }

                (kotlinComponent as? KotlinTargetComponentWithPublication)?.publicationDelegate = componentPublication
                publicationConfigureActions.all { it.execute(componentPublication) }
            }
    }

    /**
     * The metadata targets need their POMs to only include the dependencies from the commonMain API configuration.
     * The actual apiElements configurations of metadata targets now contain dependencies from all source sets, but, as the consumers who
     * can't read Gradle module metadata won't resolve a dependency on an MPP to the granular metadata variant and won't then choose the
     * right dependencies for each source set, we put only the dependencies of the legacy common variant into the POM, i.e. commonMain API.
     */
    private fun filterMetadataDependencies(target: AbstractKotlinTarget, groupNameVersion: Triple<String?, String, String?>): Boolean {
        if (target !is KotlinMetadataTarget || !target.project.isKotlinGranularMetadataEnabled) {
            return true
        }

        val (group, name, _) = groupNameVersion

        val project = target.project
        val commonMain = project.kotlinExtension.sourceSets?.findByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME)
            ?: return true

        // Only the commonMain API dependencies can be published for consumers who can't read Gradle project metadata
        val commonMainApi = project.sourceSetDependencyConfigurationByScope(commonMain, KotlinDependencyScope.API_SCOPE)

        return commonMainApi.allDependencies.any { it.group == group && it.name == name }
    }

    private fun configureSourceSets(project: Project) = with(project.multiplatformExtension) {
        val production = sourceSets.create(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME)
        val test = sourceSets.create(KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME)

        targets.all { target ->
            target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)?.let { mainCompilation ->
                mainCompilation.defaultSourceSet.takeIf { it != production }?.dependsOn(production)
            }

            target.compilations.findByName(KotlinCompilation.TEST_COMPILATION_NAME)?.let { testCompilation ->
                testCompilation.defaultSourceSet.takeIf { it != test }?.dependsOn(test)
            }

            val targetName = if (target is KotlinNativeTarget)
                target.konanTarget.name
            else
                target.platformType.name
            KotlinBuildStatsService.getInstance()?.report(StringMetrics.MPP_PLATFORMS, targetName)
        }

        UnusedSourceSetsChecker.checkSourceSets(project)

        project.whenEvaluated {
            checkSourceSetVisibilityRequirements(project)
        }
    }

    companion object {
        const val METADATA_TARGET_NAME = "metadata"

        internal fun sourceSetFreeCompilerArgsPropertyName(sourceSetName: String) =
            "kotlin.mpp.freeCompilerArgsForSourceSet.$sourceSetName"

        internal const val STABILITY_NOWARN_FLAG = "kotlin.mpp.stability.nowarn"
    }
}

/**
 * The attributes attached to the targets and compilations need to be propagated to the relevant Gradle configurations:
 * 1. Output configurations of each target need the corresponding compilation's attributes (and, indirectly, the target's attributes)
 * 2. Resolvable configurations of each compilation need the compilation's attributes
 */
internal fun applyUserDefinedAttributes(target: AbstractKotlinTarget) {
    val project = target.project

    project.whenEvaluated {
        fun copyAttributes(from: AttributeContainer, to: AttributeContainer) {
            fun <T> copyAttribute(key: Attribute<T>, from: AttributeContainer, to: AttributeContainer) {
                to.attribute(key, from.getAttribute(key)!!)
            }

            from.keySet().forEach { key -> copyAttribute(key, from, to) }
        }

        // To copy the attributes to the output configurations, find those output configurations and their producing compilations
        // based on the target's components:
        val outputConfigurationsWithCompilations =
            target.kotlinComponents.filterIsInstance<KotlinVariant>().flatMap { kotlinVariant ->
                kotlinVariant.usages.mapNotNull { usageContext ->
                    project.configurations.findByName(usageContext.dependencyConfigurationName)?.let { configuration ->
                        configuration to usageContext.compilation
                    }
                }
            } + listOfNotNull(
                target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)?.let { mainCompilation ->
                    project.configurations.findByName(target.defaultConfigurationName)?.to(mainCompilation)
                }
            )

        outputConfigurationsWithCompilations.forEach { (configuration, compilation) ->
            copyAttributes(compilation.attributes, configuration.attributes)
        }

        target.compilations.all { compilation ->
            val compilationAttributes = compilation.attributes

            compilation.relatedConfigurationNames
                .mapNotNull { configurationName -> target.project.configurations.findByName(configurationName) }
                .forEach { configuration -> copyAttributes(compilationAttributes, configuration.attributes) }
        }
    }
}

internal fun sourcesJarTask(compilation: KotlinCompilation<*>, componentName: String?, artifactNameAppendix: String): TaskProvider<Jar> =
    sourcesJarTask(compilation.target.project, lazy { compilation.allKotlinSourceSets }, componentName, artifactNameAppendix)

internal fun sourcesJarTask(
    project: Project,
    sourceSets: Lazy<Set<KotlinSourceSet>>,
    componentName: String?,
    artifactNameAppendix: String
): TaskProvider<Jar> {
    val taskName = lowerCamelCaseName(componentName, "sourcesJar")

    project.locateTask<Jar>(taskName)?.let {
        return it
    }

    val result = project.registerTask<Jar>(taskName) { sourcesJar ->
        sourcesJar.archiveAppendix.set(artifactNameAppendix)
        sourcesJar.archiveClassifier.set("sources")
    }

    project.whenEvaluated {
        result.configure {
            sourceSets.value.forEach { sourceSet ->
                it.from(sourceSet.kotlin) { copySpec ->
                    copySpec.into(sourceSet.name)
                }
            }
        }
    }

    return result
}

internal fun Project.setupGeneralKotlinExtensionParameters() {
    val sourceSetsInMainCompilation by lazy {
        CompilationSourceSetUtil.compilationsBySourceSets(project).filterValues { compilations ->
            compilations.any {
                // kotlin main compilation
                it.isMain()
                        // android compilation which is NOT in tested variant
                        || (it as? KotlinJvmAndroidCompilation)?.let { getTestedVariantData(it.androidVariant) == null } == true
            }
        }.keys
    }

    kotlinExtension.sourceSets.all { sourceSet ->
        (sourceSet.languageSettings as? DefaultLanguageSettingsBuilder)?.run {

            // Set ad-hoc free compiler args from the internal project property
            freeCompilerArgsProvider = project.provider {
                val propertyValue = with(project.extensions.extraProperties) {
                    val sourceSetFreeCompilerArgsPropertyName = sourceSetFreeCompilerArgsPropertyName(sourceSet.name)
                    if (has(sourceSetFreeCompilerArgsPropertyName)) {
                        get(sourceSetFreeCompilerArgsPropertyName)
                    } else null
                }

                mutableListOf<String>().apply {
                    when (propertyValue) {
                        is String -> add(propertyValue)
                        is Iterable<*> -> addAll(propertyValue.map { it.toString() })
                    }

                    val explicitApiState = project.kotlinExtension.explicitApi?.toCompilerArg()
                    // do not look into lazy set if explicitApiMode was not enabled
                    if (explicitApiState != null && sourceSet in sourceSetsInMainCompilation)
                        add(explicitApiState)
                }
            }
        }
    }
}
