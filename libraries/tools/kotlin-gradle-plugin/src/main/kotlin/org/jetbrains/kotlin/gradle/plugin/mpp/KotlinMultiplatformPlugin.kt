/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.internal.FeaturePreviews
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.internal.reflect.Instantiator
import org.gradle.jvm.tasks.Jar
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.configureOrCreate
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.utils.SingleWarningPerBuild
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName

internal val Project.multiplatformExtension
    get(): KotlinMultiplatformExtension? =
        project.extensions.findByName("kotlin") as? KotlinMultiplatformExtension

class KotlinMultiplatformPlugin(
    private val fileResolver: FileResolver,
    private val instantiator: Instantiator,
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

        setUpConfigurationAttributes(project)

        // set up metadata publishing
        targetsFromPreset.fromPreset(
            KotlinMetadataTargetPreset(project, instantiator, fileResolver, kotlinPluginVersion),
            METADATA_TARGET_NAME
        )
        configureSourceJars(project)

        configurePublishingWithMavenPublish(project)

        // propagate compiler plugin options to the source set language settings
        setupCompilerPluginOptions(project)

        UnusedSourceSetsChecker.checkSourceSets(project)
    }

    private fun setupCompilerPluginOptions(project: Project) {
        // common source sets use the compiler options from the metadata compilation:
        val metadataCompilation =
            project.multiplatformExtension!!.targets
                .getByName(METADATA_TARGET_NAME)
                .compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

        val primaryCompilationsBySourceSet by lazy { // don't evaluate eagerly: Android targets are not created at this point
            val allCompilationsForSourceSets = compilationsBySourceSet(project)

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
        with((project.kotlinExtension as KotlinMultiplatformExtension).presets) {
            add(KotlinJvmTargetPreset(project, instantiator, fileResolver, kotlinPluginVersion))
            add(KotlinJsTargetPreset(project, instantiator, fileResolver, kotlinPluginVersion))
            add(KotlinAndroidTargetPreset(project, kotlinPluginVersion))
            add(KotlinJvmWithJavaTargetPreset(project, kotlinPluginVersion))
            HostManager().targets.forEach { _, target ->
                add(KotlinNativeTargetPreset(target.presetName, project, target, kotlinPluginVersion))
            }
        }
    }

    private fun configurePublishingWithMavenPublish(project: Project) = project.pluginManager.withPlugin("maven-publish") { _ ->

        if (project.multiplatformExtension!!.run { isGradleMetadataAvailable && isGradleMetadataExperimental }) {
            SingleWarningPerBuild.show(
                project,
                GRADLE_METADATA_WARNING
            )
        }

        val targets = project.multiplatformExtension!!.targets
        val kotlinSoftwareComponent = KotlinSoftwareComponent("kotlin", targets)

        project.extensions.configure(PublishingExtension::class.java) { publishing ->

            // The root publication that references the platform specific publications as its variants:
            val rootPublication = publishing.publications.create("kotlinMultiplatform", MavenPublication::class.java).apply {
                artifactId = project.name.toLowerCase()
                from(kotlinSoftwareComponent)
                (this as MavenPublicationInternal).publishWithOriginalFileName()
            }

            // Publish the root publication only if Gradle metadata publishing is enabled:
            project.tasks.withType(AbstractPublishToMaven::class.java).all { publishTask ->
                publishTask.onlyIf { publishTask.publication != rootPublication || project.multiplatformExtension!!.isGradleMetadataAvailable }
            }

            // Enforce the order of creating the publications, since the metadata publication is used in the other publications:
            (targets.getByName(METADATA_TARGET_NAME) as AbstractKotlinTarget).createMavenPublications(publishing.publications)
            targets
                .withType(AbstractKotlinTarget::class.java).matching { it.publishable && it.name != METADATA_TARGET_NAME }
                .all { it.createMavenPublications(publishing.publications) }
        }

        project.components.add(kotlinSoftwareComponent)
    }

    private fun AbstractKotlinTarget.createMavenPublications(publications: PublicationContainer) {
        components
            .filter { it.publishable }
            .forEach { variant ->
                val variantPublication = publications.create(variant.name, MavenPublication::class.java).apply {
                    // do this in whenEvaluated since older Gradle versions seem to check the files in the variant eagerly:
                    project.whenEvaluated {
                        from(variant)
                        (project.tasks.findByName(sourcesJarTaskName) as Jar?)?.let { sourcesJar ->
                            artifact(sourcesJar)
                        }
                    }
                    (this as MavenPublicationInternal).publishWithOriginalFileName()
                    artifactId = variant.defaultArtifactId
                }

                (variant as? KotlinTargetComponentWithPublication)?.publicationDelegate = variantPublication
                publicationConfigureActions.all { it.execute(variantPublication) }
        }
    }

    private val KotlinTarget.sourcesJarTaskName get() = disambiguateName("sourcesJar")

    private fun configureSourceJars(project: Project) = with(project.kotlinExtension as KotlinMultiplatformExtension) {
        targets.all { target ->
            val mainCompilation = target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)
            // If a target has no `main` compilation (e.g. Android), don't create the source JAR
                ?: return@all

            val sourcesJar = project.tasks.create(target.sourcesJarTaskName, Jar::class.java) { sourcesJar ->
                sourcesJar.appendix = target.targetName.toLowerCase()
                sourcesJar.classifier = "sources"
            }

            project.afterEvaluate { _ ->
                val compiledSourceSets = mainCompilation.allKotlinSourceSets
                compiledSourceSets.forEach { sourceSet ->
                    sourcesJar.from(sourceSet.kotlin) { copySpec ->
                        copySpec.into(sourceSet.name)
                    }
                }
            }
        }
    }

    private fun configureSourceSets(project: Project) = with(project.kotlinExtension as KotlinMultiplatformExtension) {
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

    private fun setUpConfigurationAttributes(project: Project) {
        val targets = (project.kotlinExtension as KotlinMultiplatformExtension).targets

        project.afterEvaluate {
            targets.all { target ->
                val mainCompilationAttributes = target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)?.attributes
                    ?: return@all

                fun <T> copyAttribute(key: Attribute<T>, from: AttributeContainer, to: AttributeContainer) {
                    to.attribute(key, from.getAttribute(key)!!)
                }

                listOf(
                    target.apiElementsConfigurationName,
                    target.runtimeElementsConfigurationName,
                    target.defaultConfigurationName
                )
                    .mapNotNull { configurationName -> target.project.configurations.findByName(configurationName) }
                    .forEach { configuration ->
                        mainCompilationAttributes.keySet().forEach { key ->
                            copyAttribute(key, mainCompilationAttributes, configuration.attributes)
                        }
                    }

                target.compilations.all { compilation ->
                    val compilationAttributes = compilation.attributes

                    compilation.relatedConfigurationNames
                        .mapNotNull { configurationName -> target.project.configurations.findByName(configurationName) }
                        .forEach { configuration ->
                            compilationAttributes.keySet().forEach { key ->
                                copyAttribute(key, compilationAttributes, configuration.attributes)
                            }
                        }
                }
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

internal fun compilationsBySourceSet(project: Project): Map<KotlinSourceSet, Set<KotlinCompilation<*>>> =
    HashMap<KotlinSourceSet, MutableSet<KotlinCompilation<*>>>().also { result ->
        for (target in project.multiplatformExtension!!.targets) {
            for (compilation in target.compilations) {
                for (sourceSet in compilation.allKotlinSourceSets) {
                    result.getOrPut(sourceSet) { mutableSetOf() }.add(compilation)
                }
            }
        }
    }
