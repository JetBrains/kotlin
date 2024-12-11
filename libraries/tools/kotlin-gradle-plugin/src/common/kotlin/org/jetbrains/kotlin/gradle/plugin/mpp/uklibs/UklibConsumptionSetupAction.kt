/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.attributes.*
import org.gradle.api.file.*
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization.Uklib
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.uklibFragmentPlatformAttribute
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseCompilations
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import java.io.File
import javax.inject.Inject

internal val UklibConsumptionSetupAction = KotlinProjectSetupAction {
    when (project.kotlinPropertiesProvider.uklibResolutionStrategy) {
        UklibResolutionStrategy.AllowResolvingUklibs -> project.launch { setupUklibConsumption() }
        UklibResolutionStrategy.ResolveOnlyPlatformSpecificVariant -> { /* do nothing */ }
    }
}

internal val uklibStateAttribute = Attribute.of("uklibState", String::class.java)
internal val uklibStateZipped = "zipped"
internal val uklibStateUnzipped = "unzipped"

internal val uklibDestinationAttribute = Attribute.of("uklibDestination", String::class.java)
internal val uklibDestinationUnknown = "unknown"

/**
 * Resolve Uklib artifacts using transforms:
 * - Request a known [uklibDestinationAttribute] in all resolvable configurations that should be able to resolve uklibs
 * - Register transform "zipped -> unzipped uklib"
 * - Register transform "unzipped uklib -> [uklibDestinationAttribute]"
 */
private suspend fun Project.setupUklibConsumption() {
    val sourceSets = multiplatformExtension.awaitSourceSets()
    val targets = multiplatformExtension.awaitTargets()
    AfterFinaliseCompilations.await()

    registerZippedUklibArtifact()
    allowUklibsToUnzip()
    allowMetadataConfigurationsToResolveUnzippedUklib(sourceSets)
    allowPlatformCompilationsToResolvePlatformCompilationArtifactFromUklib(targets)
}

private fun Project.allowPlatformCompilationsToResolvePlatformCompilationArtifactFromUklib(
    targets: NamedDomainObjectCollection<KotlinTarget>
) {
    targets.configureEach { target ->
        val destinationAttribute = when (target) {
            is KotlinNativeTarget -> target.uklibFragmentPlatformAttribute
            is KotlinJsIrTarget -> target.uklibFragmentPlatformAttribute
            is KotlinJvmTarget -> target.uklibFragmentPlatformAttribute
            else -> return@configureEach
        }

        dependencies.registerTransform(UnzippedUklibToPlatformCompilationTransform::class.java) {
            it.from
                .attribute(uklibStateAttribute, uklibStateUnzipped)
                .attribute(uklibDestinationAttribute, uklibDestinationUnknown)
            it.to
                .attribute(uklibStateAttribute, uklibStateUnzipped)
                .attribute(uklibDestinationAttribute, destinationAttribute.unwrap())

            it.parameters.targetFragmentAttribute.set(destinationAttribute.unwrap())
            it.parameters.fakeTransform.set(kotlinPropertiesProvider.fakeUkibTransforms)
        }

        // FIXME: Refactor this and encode what configurations should be allowed to transform per KotlinTarget somewhere around [uklibFragmentPlatformAttribute]
        target.compilations.configureEach {
            listOfNotNull(
                it.internal.configurations.compileDependencyConfiguration,
                it.internal.configurations.runtimeDependencyConfiguration,
            ).forEach {
                it.attributes {
                    it.attribute(uklibStateAttribute, uklibStateUnzipped)
                    it.attribute(uklibDestinationAttribute, destinationAttribute.unwrap())
                }
            }
        }
    }
}

private fun Project.registerZippedUklibArtifact() {
    with(dependencies.artifactTypes.create(Uklib.UKLIB_EXTENSION).attributes) {
        attribute(uklibStateAttribute, uklibStateZipped)
        attribute(uklibDestinationAttribute, uklibDestinationUnknown)
    }
}

private fun Project.allowUklibsToUnzip() {
    dependencies.registerTransform(UnzipUklibTransform::class.java) {
        it.from.attribute(uklibStateAttribute, uklibStateZipped)
        it.to.attribute(uklibStateAttribute, uklibStateUnzipped)
        it.parameters.performUnzip.set(!kotlinPropertiesProvider.fakeUkibTransforms)
    }
}

private fun Project.allowMetadataConfigurationsToResolveUnzippedUklib(
    sourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
) {
    sourceSets.configureEach {
        with(it.internal.resolvableMetadataConfiguration) {
            attributes {
                it.attribute(uklibStateAttribute, uklibStateUnzipped)
                it.attribute(uklibDestinationAttribute, KotlinPlatformType.common.name)
            }
        }
    }
    dependencies.registerTransform(UnzippedUklibToMetadataCompilationTransform::class.java) {
        it.from
            .attribute(uklibStateAttribute, uklibStateUnzipped)
            .attribute(uklibDestinationAttribute, uklibDestinationUnknown)
        it.to
            .attribute(uklibStateAttribute, uklibStateUnzipped)
            .attribute(uklibDestinationAttribute, KotlinPlatformType.common.name)
    }
}


internal abstract class UnzipUklibTransform @Inject constructor(
    private val fileOperations: FileSystemOperations,
    private val archiveOperations: ArchiveOperations,
) : TransformAction<UnzipUklibTransform.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        val performUnzip: Property<Boolean>
    }

    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        // Due to kotlin-api/runtime Usages being compatible with java Usages, we might see a jar in this transform
        if (input.extension == Uklib.UKLIB_EXTENSION) {
            val outputDir = outputs.dir("unzipped_uklib_${input.name}")
            if (parameters.performUnzip.get()) {
                fileOperations.copy {
                    it.from(archiveOperations.zipTree(inputArtifact.get().asFile))
                    it.into(outputDir)
                }
            }
        }
    }
}

internal abstract class UnzippedUklibToMetadataCompilationTransform : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        outputs.dir(inputArtifact.get().asFile)
    }
}

internal abstract class UnzippedUklibToPlatformCompilationTransform : TransformAction<UnzippedUklibToPlatformCompilationTransform.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        val targetFragmentAttribute: Property<String>

        @get:Input
        val fakeTransform: Property<Boolean>
    }

    internal class PlatformCompilationTransformException(
        val unzippedUklib: File,
        val targetFragmentAttribute: String,
        val availablePlatformFragments: List<String>,
    ) : IllegalStateException(
        "Couldn't resolve platform compilation artifact from $unzippedUklib failed. Needed fragment with attribute '${targetFragmentAttribute}', but only the following fragments were available $availablePlatformFragments"
    )

    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        if (parameters.fakeTransform.get()) {
            outputs.dir(inputArtifact.get().asFile)
            return
        }

        val unzippedUklib = inputArtifact.get().asFile
        val targetFragmentAttribute = parameters.targetFragmentAttribute.get()
        // FIXME: Build up a Set<Attribute> -> Fragment map instead?
        val uklib = Uklib.deserializeFromDirectory(unzippedUklib)
        val platformFragments = uklib
            .module.fragments
            .filter { it.attributes == setOf(targetFragmentAttribute) }

        if (platformFragments.isEmpty()) {
            /**
             * FIXME: Uklib spec mentions that there may be an intermediate fragment without refiners. Was this a crutch for kotlin-test? Should we check this case silently ignore this case here?
             */
            throw PlatformCompilationTransformException(unzippedUklib, targetFragmentAttribute, uklib.module.fragments.map { it.identifier }.sorted())
        }

        if (platformFragments.size > 1) {
            error("Matched multiple fragments from ${unzippedUklib}, but was expecting to find exactly one. Found fragments: ${platformFragments}")
        }

        outputs.dir(platformFragments.single().file())
    }
}

internal enum class UklibResolutionStrategy {
    AllowResolvingUklibs,
    ResolveOnlyPlatformSpecificVariant;

    val propertyName: String
        get() = when (this) {
            AllowResolvingUklibs -> "allowResolvingUklibs"
            ResolveOnlyPlatformSpecificVariant -> "resolveOnlyPlatformSpecificVariant"
        }

    companion object {
        fun fromProperty(name: String): UklibResolutionStrategy? = when (name) {
            AllowResolvingUklibs.propertyName -> AllowResolvingUklibs
            ResolveOnlyPlatformSpecificVariant.propertyName -> ResolveOnlyPlatformSpecificVariant
            else -> null
        }
    }
}