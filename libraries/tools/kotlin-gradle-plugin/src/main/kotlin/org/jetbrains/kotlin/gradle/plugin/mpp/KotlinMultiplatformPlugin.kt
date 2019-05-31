/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.configureOrCreate
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName

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
        checkGradleCompatibility()

        project.plugins.apply(JavaBasePlugin::class.java)
        SingleWarningPerBuild.show(project, "Kotlin Multiplatform Projects are an experimental feature.")

        val targetsContainer = project.container(KotlinTarget::class.java)
        val kotlinMultiplatformExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        val targetsFromPreset = TargetFromPresetExtension(kotlinMultiplatformExtension)

        kotlinMultiplatformExtension.apply {
            DslObject(targetsContainer).addConvention("fromPreset", targetsFromPreset)

            targets = targetsContainer
            addExtension("targets", targets)

            presets = project.container(KotlinTargetPreset::class.java)
            addExtension("presets", presets)

            isGradleMetadataAvailable =
                    featurePreviews.activeFeatures.find { it.name == "GRADLE_METADATA" }?.let { metadataFeature ->
                        isGradleMetadataExperimental = true
                        featurePreviews.isFeatureEnabled(metadataFeature)
                    } ?: true // the feature entry will be gone once the feature is stable
        }

        setupDefaultPresets(project)
        configureDefaultVersionsResolutionStrategy(project, kotlinPluginVersion)
        configureSourceSets(project)

        // set up metadata publishing
        targetsFromPreset.fromPreset(
            KotlinMetadataTargetPreset(project, kotlinPluginVersion),
            METADATA_TARGET_NAME
        )
        configurePublishingWithMavenPublish(project)

        targetsContainer.withType(AbstractKotlinTarget::class.java).all { applyUserDefinedAttributes(it) }

        // propagate compiler plugin options to the source set language settings
        setupCompilerPluginOptions(project)

        project.pluginManager.apply(ScriptingGradleSubplugin::class.java)

        UnusedSourceSetsChecker.checkSourceSets(project)
    }

    private fun setupCompilerPluginOptions(project: Project) {
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
            add(KotlinJsTargetPreset(project, kotlinPluginVersion))
            add(KotlinAndroidTargetPreset(project, kotlinPluginVersion))
            add(KotlinJvmWithJavaTargetPreset(project, kotlinPluginVersion))
            HostManager().targets.forEach { _, target ->
                add(KotlinNativeTargetPreset(target.presetName, project, target, kotlinPluginVersion))
            }
        }
    }

    private fun configurePublishingWithMavenPublish(project: Project) = project.pluginManager.withPlugin("maven-publish") { _ ->

        if (project.multiplatformExtension.run { isGradleMetadataAvailable && isGradleMetadataExperimental }) {
            SingleWarningPerBuild.show(
                project,
                GRADLE_METADATA_WARNING
            )
        }

        val targets = project.multiplatformExtension.targets
        val kotlinSoftwareComponent = project.multiplatformExtension.rootSoftwareComponent

        project.extensions.configure(PublishingExtension::class.java) { publishing ->

            // The root publication that references the platform specific publications as its variants:
            val rootPublication = publishing.publications.create("kotlinMultiplatform", MavenPublication::class.java).apply {
                from(kotlinSoftwareComponent)
                (this as MavenPublicationInternal).publishWithOriginalFileName()
                kotlinSoftwareComponent.publicationDelegate = this@apply
            }

            // Publish the root publication only if Gradle metadata publishing is enabled:
            project.tasks.withType(AbstractPublishToMaven::class.java).all { publishTask ->
                publishTask.onlyIf { publishTask.publication != rootPublication || project.multiplatformExtension.isGradleMetadataAvailable }
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
        val metadataApiLegacyElements = project.configurations.getByName(COMMON_MAIN_ELEMENTS_CONFIGURATION_NAME)

        return metadataApiLegacyElements.allDependencies.any { it.group == group && it.name == name }
    }

    private fun configureSourceSets(project: Project) = with(project.multiplatformExtension) {
        val production = sourceSets.create(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME)
        val test = sourceSets.create(KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME)

        targets.all { target ->
            target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)?.let { mainCompilation ->
                sourceSets.findByName(mainCompilation.defaultSourceSetName)?.dependsOn(production)
            }

            target.compilations.findByName(KotlinCompilation.TEST_COMPILATION_NAME)?.let { testCompilation ->
                sourceSets.findByName(testCompilation.defaultSourceSetName)?.dependsOn(test)
            }
        }
    }

    companion object {
        const val METADATA_TARGET_NAME = "metadata"

        const val GRADLE_METADATA_WARNING =
        // TODO point the user to some MPP docs explaining this in more detail
            "This build is set up to publish Kotlin multiplatform libraries with experimental Gradle metadata. " +
                    "Future Gradle versions may fail to resolve dependencies on these publications. " +
                    "You can disable Gradle metadata usage during publishing and dependencies resolution by removing " +
                    "`enableFeaturePreview('GRADLE_METADATA')` from the settings.gradle file."
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

internal fun sourcesJarTask(compilation: KotlinCompilation<*>, componentName: String?, artifactNameAppendix: String): Jar =
    sourcesJarTask(compilation.target.project, lazy { compilation.allKotlinSourceSets }, componentName, artifactNameAppendix)

internal fun sourcesJarTask(
    project: Project,
    sourceSets: Lazy<Set<KotlinSourceSet>>,
    componentName: String?,
    artifactNameAppendix: String
): Jar {
    val taskName = lowerCamelCaseName(componentName, "sourcesJar")

    (project.tasks.findByName(taskName) as? Jar)?.let { return it }

    val result = project.tasks.create(taskName, Jar::class.java) { sourcesJar ->
        sourcesJar.setArchiveAppendixCompatible { artifactNameAppendix }
        sourcesJar.setArchiveClassifierCompatible { "sources" }
    }

    project.whenEvaluated {
        sourceSets.value.forEach { sourceSet ->
            result.from(sourceSet.kotlin) { copySpec ->
                copySpec.into(sourceSet.name)
            }
        }
    }

    return result
}