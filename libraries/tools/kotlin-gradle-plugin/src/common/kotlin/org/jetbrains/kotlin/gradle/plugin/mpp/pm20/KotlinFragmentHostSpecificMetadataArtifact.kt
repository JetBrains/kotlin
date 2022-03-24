/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Dependency
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.disambiguateName
import org.jetbrains.kotlin.gradle.targets.metadata.filesWithUnpackedArchives
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.project.model.withRefinesClosure

/**
 * Will register a 'hostSpecificMetadataJar' [Jar] task containing compilation outputs of host specific metadata.
 * Will add this jar artifact to the given configuration
 */

val KotlinFragmentHostSpecificMetadataArtifact = FragmentArtifacts<KotlinNativeVariantInternal> artifacts@{
    val hostSpecificMetadataElements = fragment.hostSpecificMetadataElementsConfiguration ?: return@artifacts

    val hostSpecificMetadataJar = project.registerTask<Jar>(fragment.disambiguateName("hostSpecificMetadataJar")) { jar ->
        jar.archiveClassifier.set("metadata")
        jar.archiveAppendix.set(fragment.disambiguateName(""))
        project.metadataCompilationRegistryByModuleId.getValue(fragment.containingModule.moduleIdentifier)
            .withAll { metadataCompilation ->
                val metadataFragment = metadataCompilation.fragment
                if (metadataCompilation is KotlinNativeFragmentMetadataCompilationData) {
                    jar.from(project.files(project.provider {
                        if (metadataFragment in fragment.withRefinesClosure && metadataFragment.isNativeHostSpecific())
                            project.filesWithUnpackedArchives(metadataCompilation.output.allOutputs, setOf(KLIB_FILE_EXTENSION))
                        else emptyList<Any>()
                    })) { spec -> spec.into(metadataFragment.name) }
                }
            }
    }

    artifact(hostSpecificMetadataJar)
    hostSpecificMetadataElements.dependencies.addAllLater(project.objects.listProperty(Dependency::class.java).apply {
        set(project.provider { fragment.apiElementsConfiguration.allDependencies })
    })
}
