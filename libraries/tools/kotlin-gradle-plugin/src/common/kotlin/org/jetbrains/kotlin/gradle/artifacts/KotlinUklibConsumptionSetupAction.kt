/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.Project
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.attributes.*
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.file.*
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.artifacts.uklibsPublication.setupPublication
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseCompilations
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.publishing.configureResourcesPublicationAttributes
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.utils.named
import org.jetbrains.kotlin.gradle.utils.setAttribute
import javax.inject.Inject

private val artifactType: Attribute<String> get() = Attribute.of("artifactType", String::class.java)
private val uklibArtifactType: String get() = "uklib"
private val uklibUnzippedArtifactType: String get() = "uklib-unzipped"

internal val KotlinUklibConsumptionSetupAction = KotlinProjectSetupAction {

    if (project.kotlinPropertiesProvider.publishUklibVariant) {
        project.launch {
            setupPublication()
        }
    }

    when (project.kotlinPropertiesProvider.uklibResolutionStrategy) {
        UklibResolutionStrategy.PreferUklibVariant,
        UklibResolutionStrategy.PreferPlatformSpecificVariant -> project.launch { setupConsumption() }
        UklibResolutionStrategy.ResolveOnlyPlatformSpecificVariant -> { /* do nothing */ }
    }
}

val uklibStateAttribute = Attribute.of("uklibState", String::class.java)
val uklibStateZipped = "zipped"
val uklibStateUnzipped = "unzipped"

val uklibPlatformAttribute = Attribute.of("uklibPlatform", String::class.java)
val uklibPlatformUnknown = "unknown"
val uklibNativeSliceAttribute = Attribute.of("uklibNativeSlice", String::class.java)
val uklibNativeSliceUnknown = "unknown"

private suspend fun Project.setupConsumption() {
    val sourceSets = multiplatformExtension.awaitSourceSets()
    val metadataTarget = multiplatformExtension.awaitMetadataTarget()
    val targets = multiplatformExtension.awaitTargets()
    AfterFinaliseCompilations.await()

    sourceSets.configureEach {
        with(it.internal.resolvableMetadataConfiguration) {
            attributes {
                when (project.kotlinPropertiesProvider.uklibResolutionStrategy) {
                    UklibResolutionStrategy.PreferUklibVariant -> it.attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_UKLIB))
                    UklibResolutionStrategy.PreferPlatformSpecificVariant -> {
                        /* rely on the default + compatibility rule */
                    }
                }
                it.attribute(uklibStateAttribute, uklibStateUnzipped)
                it.attribute(uklibPlatformAttribute, KotlinPlatformType.common.name)
            }
        }
    }
    // FIXME: Drop this transform and use the unzip transform instead
    dependencies.registerTransform(UnzippedUklibToMetadataCompilationTransform::class.java) {
        it.from
            .attribute(uklibStateAttribute, uklibStateUnzipped)
            .attribute(uklibPlatformAttribute, uklibPlatformUnknown)
        it.to
            .attribute(uklibStateAttribute, uklibStateUnzipped)
            .attribute(uklibPlatformAttribute, KotlinPlatformType.common.name)
    }

    targets.configureEach { target ->
        if (target is KotlinMetadataTarget) return@configureEach
        if (target is KotlinNativeTarget) {
            dependencies.registerTransform(UnzippedUklibToPlatformCompilationTransform::class.java) {
                it.from
                    .attribute(uklibStateAttribute, uklibStateUnzipped)
                    .attribute(uklibPlatformAttribute, uklibPlatformUnknown)
                    .attribute(uklibNativeSliceAttribute, uklibNativeSliceUnknown)
                it.to
                    .attribute(uklibStateAttribute, uklibStateUnzipped)
                    .attribute(uklibPlatformAttribute, target.platformType.name)
                    .attribute(uklibNativeSliceAttribute, target.targetName)

                it.parameters.targetAttributes.set(setOf(target.targetName))
                it.parameters.fakeTransform.set(kotlinPropertiesProvider.fakeUkibTransforms)
            }
            target.compilations.configureEach {
                listOfNotNull(
                    it.internal.configurations.compileDependencyConfiguration,
                    it.internal.configurations.runtimeDependencyConfiguration,
                ).forEach { config ->
                    with(config.attributes) {
                        when (project.kotlinPropertiesProvider.uklibResolutionStrategy) {
                            UklibResolutionStrategy.PreferUklibVariant -> attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_UKLIB))
                            UklibResolutionStrategy.PreferPlatformSpecificVariant -> {
                                /* rely on the default + compatibility rule */
                            }
                        }
                        attribute(uklibStateAttribute, uklibStateUnzipped)
                        attribute(uklibPlatformAttribute, target.platformType.name)
                        attribute(uklibNativeSliceAttribute, target.targetName)
                    }
                }
            }
        } else {
            dependencies.registerTransform(UnzippedUklibToPlatformCompilationTransform::class.java) {
                it.from
                    .attribute(uklibStateAttribute, uklibStateUnzipped)
                    .attribute(uklibPlatformAttribute, uklibPlatformUnknown)
                it.to
                    .attribute(uklibStateAttribute, uklibStateUnzipped)
                    .attribute(uklibPlatformAttribute, target.platformType.name)

                it.parameters.targetAttributes.set(setOf(target.targetName))
                it.parameters.fakeTransform.set(kotlinPropertiesProvider.fakeUkibTransforms)
            }
            target.compilations.configureEach {
                listOfNotNull(
                    it.internal.configurations.compileDependencyConfiguration,
                    it.internal.configurations.runtimeDependencyConfiguration,
                ).forEach { config ->
                    with(config.attributes) {
                        when (project.kotlinPropertiesProvider.uklibResolutionStrategy) {
                            UklibResolutionStrategy.PreferUklibVariant -> attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_UKLIB))
                            UklibResolutionStrategy.PreferPlatformSpecificVariant -> {
                                /* rely on the default + compatibility rule */
                            }
                        }

                        attribute(uklibStateAttribute, uklibStateUnzipped)
                        attribute(uklibPlatformAttribute, target.platformType.name)
                    }
                }
            }
        }
    }

    with(dependencies.artifactTypes.create("uklib").attributes) {
        attribute(uklibStateAttribute, uklibStateZipped)
        attribute(uklibPlatformAttribute, uklibPlatformUnknown)
        attribute(uklibNativeSliceAttribute, uklibNativeSliceUnknown)
    }

    dependencies.registerTransform(UklibUnzipTransform::class.java) {
        it.from.attribute(uklibStateAttribute, uklibStateZipped)
        it.to.attribute(uklibStateAttribute, uklibStateUnzipped)
        it.parameters.performUnzip.set(!kotlinPropertiesProvider.fakeUkibTransforms)
    }

    dependencies.attributesSchema.attribute(Usage.USAGE_ATTRIBUTE).compatibilityRules.add(
        MakeUklibCompatibleWithPlatformCompilations::class.java
    )
    dependencies.attributesSchema.attribute(Usage.USAGE_ATTRIBUTE).compatibilityRules.add(
        MakePlatformCompilationsCompatibleWithUklibCompilations::class.java
    )
    dependencies.attributesSchema.attribute(Usage.USAGE_ATTRIBUTE).disambiguationRules.add(
        MakeJavaRuntimePreferableForUklibCompilations::class.java
    )

    dependencies.attributesSchema.attribute(KotlinPlatformType.attribute).compatibilityRules.add(
        MakePlatformCompatibleWithUklibs::class.java
    )
    dependencies.attributesSchema.attribute(KotlinNativeTarget.konanTargetAttribute).compatibilityRules.add(
        MakeKNSliceCompatibleWithUklibs::class.java
    )
    dependencies.attributesSchema.attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE).compatibilityRules.add(
        MakeTargetJvmEnvironmentCompatibleWithUklibs::class.java
    )

}

abstract class UklibUnzipTransform @Inject constructor(
    private val fileOperations: FileSystemOperations,
    private val archiveOperations: ArchiveOperations,
) : TransformAction<UklibUnzipTransform.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        val performUnzip: Property<Boolean>
    }

    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        // FIXME: 25.11.2024 - When resolving a jar with uklib packaging Gradle comes here with the jar
        val input = inputArtifact.get().asFile
        if (input.extension == "uklib") {
            val outputDir = outputs.dir("unzipped_uklib_${input.name}")
            // FIXME: 13.11.2024 - Throw this away because this is not really testable with UT?
            if (parameters.performUnzip.get()) {
                fileOperations.copy {
                    it.from(archiveOperations.zipTree(inputArtifact.get().asFile))
                    it.into(outputDir)
                }
            }
        }
    }
}

abstract class UnzippedUklibToMetadataCompilationTransform @Inject constructor() : TransformAction<UnzippedUklibToMetadataCompilationTransform.Parameters> {
    interface Parameters : TransformParameters {}

    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        outputs.dir(inputArtifact.get().asFile)
    }
}

abstract class UnzippedUklibToPlatformCompilationTransform @Inject constructor(
) : TransformAction<UnzippedUklibToPlatformCompilationTransform.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        val targetAttributes: SetProperty<String>

        @get:Input
        val fakeTransform: Property<Boolean>
    }

    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        if (parameters.fakeTransform.get()) {
            outputs.dir(inputArtifact.get().asFile)
            return
        }

        val unzippedUklib = inputArtifact.get().asFile

        val targetAttributes = parameters.targetAttributes.get()
        val uklib = Uklib.deserializeFromDirectory(unzippedUklib)
        // FIXME: Build up a Set<Attribute> -> Fragment instead?
        val platformFragments = uklib.module.fragments.filter { it.attributes == targetAttributes }

        if (platformFragments.isEmpty()) {
            // The fragment is just not there. Skip this dependency
            return
        }

        if (platformFragments.size > 1) {
            error("Somehow more than one platform fragment matches platform attributes: ${platformFragments}")
        }

        outputs.dir(uklib.fragmentToArtifact[platformFragments.single().identifier]!!)
    }
}

internal fun HasAttributes.configureUklibConfigurationAttributes(project: Project) {
    attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
    attributes.setAttribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_UKLIB))

    attributes.setAttribute(KotlinPlatformType.attribute, KotlinPlatformType.unknown)
    attributes.setAttribute(KotlinNativeTarget.konanTargetAttribute, "???")
    attributes.setAttribute(KlibPackaging.ATTRIBUTE, project.objects.named(KlibPackaging.PACKED))
    attributes.setAttribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.objects.named("???"))
}


class MakeUklibCompatibleWithPlatformCompilations : AttributeCompatibilityRule<Usage> {
    override fun execute(t: CompatibilityCheckDetails<Usage>) {
        val allowedConsumers = setOf(
            KotlinUsages.KOTLIN_API,
            Usage.JAVA_API,
            Usage.JAVA_RUNTIME,
        )

        if (t.producerValue?.name == KotlinUsages.KOTLIN_UKLIB && t.consumerValue?.name in allowedConsumers) {
            t.compatible()
        }
    }
}

class MakePlatformCompilationsCompatibleWithUklibCompilations : AttributeCompatibilityRule<Usage> {
    override fun execute(t: CompatibilityCheckDetails<Usage>) {
        val allowedProducers = setOf(
            KotlinUsages.KOTLIN_API,
            Usage.JAVA_API,
            Usage.JAVA_RUNTIME,
        )

        if (t.producerValue?.name in allowedProducers && t.consumerValue?.name == KotlinUsages.KOTLIN_UKLIB) {
            t.compatible()
        }
    }
}

class MakeKNSliceCompatibleWithUklibs : AttributeCompatibilityRule<String> {
    override fun execute(t: CompatibilityCheckDetails<String>) {
        if (t.producerValue == "???") {
            t.compatible()
        }
    }
}

class MakeTargetJvmEnvironmentCompatibleWithUklibs : AttributeCompatibilityRule<TargetJvmEnvironment> {
    override fun execute(t: CompatibilityCheckDetails<TargetJvmEnvironment>) {
        if (t.producerValue?.name == "???") {
            t.compatible()
        }
    }
}


class MakePlatformCompatibleWithUklibs : AttributeCompatibilityRule<KotlinPlatformType> {
    override fun execute(t: CompatibilityCheckDetails<KotlinPlatformType>) {
        if (t.producerValue == KotlinPlatformType.unknown) {
            t.compatible()
        }
    }
}

class MakeJavaRuntimePreferableForUklibCompilations : AttributeDisambiguationRule<Usage> {
    override fun execute(p0: MultipleCandidatesDetails<Usage>) {
        if (p0.consumerValue?.name == KotlinUsages.KOTLIN_UKLIB) {
            p0.candidateValues.firstOrNull { it.name == Usage.JAVA_RUNTIME }?.let {
                p0.closestMatch(it)
            }
        }
    }
}

enum class UklibResolutionStrategy {
    PreferUklibVariant,
    PreferPlatformSpecificVariant,
    ResolveOnlyPlatformSpecificVariant;

    val propertyName: String
        get() = when (this) {
            PreferUklibVariant -> "preferUklibVariant"
            PreferPlatformSpecificVariant -> "preferPlatformSpecificVariant"
            ResolveOnlyPlatformSpecificVariant -> "resolveOnlyPlatformSpecificVariant"
        }

    companion object {
        fun fromProperty(name: String): UklibResolutionStrategy? = when (name) {
            PreferUklibVariant.propertyName -> PreferUklibVariant
            PreferPlatformSpecificVariant.propertyName -> PreferPlatformSpecificVariant
            ResolveOnlyPlatformSpecificVariant.propertyName -> ResolveOnlyPlatformSpecificVariant
            else -> null
        }
    }
}