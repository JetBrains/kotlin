/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import com.android.build.gradle.api.BaseVariant
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchy.SourceSetTree
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.copyAttributes
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.gradle.utils.forAllAndroidVariants
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.setProperty
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.addIfNotNull
import javax.inject.Inject

abstract class KotlinAndroidTarget @Inject constructor(
    final override val targetName: String,
    project: Project,
) : AbstractKotlinTarget(project) {

    final override val disambiguationClassifier: String = targetName

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.androidJvm

    override val compilations: NamedDomainObjectContainer<out KotlinJvmAndroidCompilation> =
        project.container(KotlinJvmAndroidCompilation::class.java)


    @ExperimentalKotlinGradlePluginApi
    val mainVariant: KotlinAndroidTargetVariantDsl = KotlinAndroidTargetVariantDslImpl(project.objects).apply {
        sourceSetTree.convention(SourceSetTree.main)
    }

    @ExperimentalKotlinGradlePluginApi
    fun mainVariant(action: Action<KotlinAndroidTargetVariantDsl>) {
        action.execute(mainVariant)
    }

    @ExperimentalKotlinGradlePluginApi
    fun mainVariant(configure: KotlinAndroidTargetVariantDsl.() -> Unit) {
        mainVariant.configure()
    }

    @ExperimentalKotlinGradlePluginApi
    val unitTestVariant: KotlinAndroidTargetVariantDsl = KotlinAndroidTargetVariantDslImpl(project.objects).apply {
        sourceSetTree.convention(SourceSetTree.test)
    }

    @ExperimentalKotlinGradlePluginApi
    fun unitTestVariant(action: Action<KotlinAndroidTargetVariantDsl>) {
        action.execute(unitTestVariant)
    }

    @ExperimentalKotlinGradlePluginApi
    fun unitTestVariant(configure: KotlinAndroidTargetVariantDsl.() -> Unit) {
        unitTestVariant.configure()
    }

    @ExperimentalKotlinGradlePluginApi
    val instrumentedTestVariant: KotlinAndroidTargetVariantDsl = KotlinAndroidTargetVariantDslImpl(project.objects).apply {
        sourceSetTree.convention(SourceSetTree.instrumentedTest)
    }

    @ExperimentalKotlinGradlePluginApi
    fun instrumentedTestVariant(action: Action<KotlinAndroidTargetVariantDsl>) {
        action.execute(instrumentedTestVariant)
    }

    @ExperimentalKotlinGradlePluginApi
    fun instrumentedTestVariant(configure: KotlinAndroidTargetVariantDsl.() -> Unit) {
        instrumentedTestVariant.configure()
    }


    /** Names of the Android library variants that should be published from the target's project within the default publications which are
     * set up if the `maven-publish` Gradle plugin is applied.
     *
     * Item examples:
     * * 'release' (in case no product flavors were defined)
     * * 'fooRelease' (for the release build type of a flavor 'foo')
     * * 'fooBarRelease' (for the release build type multi-dimensional flavors 'foo' and 'bar').
     *
     * If set to null, which can also be done with [publishAllLibraryVariants],
     * all library variants will be published, but not test or application variants. */
    var publishLibraryVariants: List<String>? = listOf()
        // Workaround for Groovy GString items in a list:
        set(value) {
            field = value?.map(Any::toString)
        }

    /** Add Android library variant names to [publishLibraryVariants]. */
    fun publishLibraryVariants(vararg names: String) {
        publishLibraryVariants = publishLibraryVariants.orEmpty() + names
    }

    /** Set up all of the Android library variants to be published from this target's project within the default publications, which are
     * set up if the `maven-publish` Gradle plugin is applied. This overrides the variants chosen with [publishLibraryVariants] */
    fun publishAllLibraryVariants() {
        publishLibraryVariants = null
    }

    /** If true, a publication will be created per merged product flavor, with the build types used as classifiers for the artifacts
     * published within each publication. If set to false, each Android variant will have a separate publication. */
    var publishLibraryVariantsGroupedByFlavor = false

    private fun checkPublishLibraryVariantsExist() {
        fun AndroidProjectHandler.getLibraryVariantNames() =
            mutableSetOf<String>().apply {
                project.forAllAndroidVariants {
                    if (getLibraryOutputTask(it) != null)
                        add(getVariantName(it))
                }
            }

        val variantNames = KotlinAndroidPlugin.androidTargetHandler().getLibraryVariantNames()

        val missingVariants =
            publishLibraryVariants?.minus(variantNames).orEmpty()

        if (missingVariants.isNotEmpty())
            throw InvalidUserDataException(
                "Kotlin target '$targetName' tried to set up publishing for Android build variants that are not library variants " +
                        "or do not exist:\n" + missingVariants.joinToString("\n") { "* $it" } +
                        "\nCheck the 'publishLibraryVariants' property, it should point to existing Android library variants. Publishing " +
                        "of application and test variants is not supported."
            )
    }

    override val kotlinComponents by lazy {
        checkPublishLibraryVariantsExist()

        KotlinAndroidPlugin.androidTargetHandler().doCreateComponents()
    }

    private fun isVariantPublished(variant: BaseVariant): Boolean {
        return publishLibraryVariants?.contains(getVariantName(variant)) ?: true
    }

    private fun AndroidProjectHandler.doCreateComponents(): Set<KotlinTargetComponent> {

        val publishableVariants = mutableListOf<BaseVariant>()
            .apply { project.forAllAndroidVariants { add(it) } }
            .toList() // Defensive copy against unlikely modification by the lambda that captures the list above in forEachVariant { }
            .filter { getLibraryOutputTask(it) != null }

        val publishableVariantGroups = publishableVariants.groupBy { variant ->
            val flavorNames = getFlavorNames(variant)
            if (publishLibraryVariantsGroupedByFlavor) {
                // For each flavor, we group its variants (which differ only in the build type) in a single component in order to publish
                // all of the build types of the flavor as a single module with the build type as the classifier of the artifacts
                flavorNames
            } else {
                flavorNames + getBuildTypeName(variant)
            }
        }

        return publishableVariantGroups.map { (flavorGroupNameParts, androidVariants) ->
            val nestedVariants = androidVariants.mapTo(mutableSetOf()) { androidVariant ->
                val androidVariantName = getVariantName(androidVariant)
                val compilation = compilations.getByName(androidVariantName)

                val usageContexts = createAndroidUsageContexts(
                    variant = androidVariant,
                    compilation = compilation,
                    isSingleBuildType = publishableVariants.filter(::isVariantPublished).map(::getBuildTypeName).distinct().size == 1,
                )

                createKotlinVariant(
                    lowerCamelCaseName(compilation.target.name, *flavorGroupNameParts.toTypedArray()),
                    compilation,
                    usageContexts,
                ).apply {
                    publishable = isVariantPublished(androidVariant)

                    if (!publishLibraryVariantsGroupedByFlavor) {
                        defaultArtifactIdSuffix =
                            dashSeparatedName(
                                (getFlavorNames(androidVariant) + getBuildTypeName(androidVariant).takeIf { it != "release" })
                                    .map { it?.toLowerCaseAsciiOnly() }
                            ).takeIf { it.isNotEmpty() }
                    }
                }
            }

            if (publishLibraryVariantsGroupedByFlavor) {
                JointAndroidKotlinTargetComponent(
                    target = this@KotlinAndroidTarget,
                    nestedVariants = nestedVariants,
                    flavorNames = flavorGroupNameParts,
                )
            } else {
                nestedVariants.single()
            }
        }.toSet()
    }

    private fun AndroidProjectHandler.createAndroidUsageContexts(
        variant: BaseVariant,
        compilation: KotlinCompilation<*>,
        isSingleBuildType: Boolean,
    ): Set<DefaultKotlinUsageContext> {
        val flavorNames = getFlavorNames(variant)
        val buildTypeName = getBuildTypeName(variant)
        val artifactClassifier = buildTypeName.takeIf { it != "release" && publishLibraryVariantsGroupedByFlavor }

        val variantName = getVariantName(variant)
        val outputTaskOrProvider = getLibraryOutputTask(variant) ?: return emptySet()
        val artifact = run {
            val archivesConfigurationName = lowerCamelCaseName(targetName, variantName, "archives")
            project.configurations.maybeCreate(archivesConfigurationName).apply {
                isCanBeConsumed = false
                isCanBeResolved = false
            }
            project.artifacts.add(archivesConfigurationName, outputTaskOrProvider) { artifact ->
                artifact.classifier = artifactClassifier
            }
        }

        val apiElementsConfigurationName = lowerCamelCaseName(variantName, "apiElements")
        val runtimeElementsConfigurationName = lowerCamelCaseName(variantName, "runtimeElements")

        val sourcesElementsConfigurationName = lowerCamelCaseName(variantName, "sourcesElements")
        val sourcesElementsConfiguration = createSourcesElementsIfNeeded(
            variantName,
            apiElementsConfigurationName,
            sourcesElementsConfigurationName
        )

        fun AttributeContainer.filterOutAndroidVariantAttributes(): AttributeContainer =
            HierarchyAttributeContainer(this) {
                val valueString = run {
                    val value = getAttribute(it)
                    (value as? Named)?.name ?: value.toString()
                }
                filterOutAndroidVariantAttribute(it) &&
                        filterOutAndroidBuildTypeAttribute(it, valueString, isSingleBuildType) &&
                        filterOutAndroidAgpVersionAttribute(it)
            }

        val sourcesUsageContext = createSourcesJarAndUsageContextIfPublishable(
            producingCompilation = compilation,
            componentName = compilation.disambiguateName(""),
            artifactNameAppendix = dashSeparatedName(
                compilation.target.name.toLowerCaseAsciiOnly(),
                *flavorNames.map { it.toLowerCaseAsciiOnly() }.toTypedArray(),
                buildTypeName.takeIf { it != "release" }?.toLowerCaseAsciiOnly()
            ),
            classifierPrefix = artifactClassifier,
            sourcesElementsConfigurationName = sourcesElementsConfigurationName,
            overrideConfigurationAttributes = sourcesElementsConfiguration.attributes.filterOutAndroidVariantAttributes()
        )

        val usageContexts = listOf(
            apiElementsConfigurationName to KotlinUsageContext.MavenScope.COMPILE,
            runtimeElementsConfigurationName to KotlinUsageContext.MavenScope.RUNTIME,
        ).mapTo(mutableSetOf()) { (dependencyConfigurationName, mavenScope) ->
            val configuration = project.configurations.getByName(dependencyConfigurationName)
            DefaultKotlinUsageContext(
                compilation,
                mavenScope,
                dependencyConfigurationName,
                overrideConfigurationArtifacts = project.setProperty { listOf(artifact) },
                overrideConfigurationAttributes = configuration.attributes.filterOutAndroidVariantAttributes()
            )
        }
        usageContexts.addIfNotNull(sourcesUsageContext)

        return usageContexts
    }

    /**
     * TODO: Ask Google about providing such configuration where they could set their attributes and control them.
     * Just like as they do with apiElements or runtimeElements
     */
    private fun createSourcesElementsIfNeeded(
        variantName: String,
        apiElementsConfigurationName: String,
        sourcesElementsConfigurationName: String,
    ): Configuration {
        val existingConfiguration = project.configurations.findByName(sourcesElementsConfigurationName)
        if (existingConfiguration != null) return existingConfiguration

        val apiElementsConfiguration = project.configurations.getByName(apiElementsConfigurationName)
        return project.configurations.create(sourcesElementsConfigurationName).apply {
            description = "Source files of Android ${variantName}."
            isVisible = false
            isCanBeResolved = false
            isCanBeConsumed = true

            copyAttributes(apiElementsConfiguration.attributes, attributes)
            configureSourcesPublicationAttributes(this@KotlinAndroidTarget)
        }
    }

    /** We filter this variant out as it is never requested on the consumer side, while keeping it leads to ambiguity between Android and
     * JVM variants due to non-nesting sets of unmatched attributes. */
    private fun filterOutAndroidVariantAttribute(
        attribute: Attribute<*>,
    ): Boolean =
        attribute.name != "com.android.build.gradle.internal.attributes.VariantAttr" &&
                attribute.name != "com.android.build.api.attributes.VariantAttr"

    private fun filterOutAndroidBuildTypeAttribute(
        it: Attribute<*>,
        valueString: String,
        isSinglePublishedVariant: Boolean,
    ) = when {
        PropertiesProvider(project).keepAndroidBuildTypeAttribute -> true
        it.name != "com.android.build.api.attributes.BuildTypeAttr" -> true

        // then the name is "com.android.build.api.attributes.BuildTypeAttr", so we omit it if there's just the single variant and always for the release one:
        valueString == "release" -> false
        isSinglePublishedVariant -> false
        else -> true
    }

    private fun filterOutAndroidAgpVersionAttribute(
        attribute: Attribute<*>,
    ): Boolean = attribute.name != "com.android.build.api.attributes.AgpVersionAttr"
}

