/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Dependency
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.disambiguateName
import org.jetbrains.kotlin.gradle.targets.metadata.filesWithUnpackedArchives
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.project.model.refinesClosure

object KotlinHostSpecificMetadataArtifactConfigurator : KotlinGradleFragmentFactory.FragmentConfigurator<KotlinNativeVariantInternal> {
    override fun configure(fragment: KotlinNativeVariantInternal) {
        val project = fragment.project
        val hostSpecificMetadataElements = fragment.hostSpecificMetadataElementsConfiguration ?: return

        val hostSpecificMetadataJar = project.registerTask<Jar>(fragment.disambiguateName("hostSpecificMetadataJar")) { jar ->
            jar.archiveClassifier.set("metadata")
            jar.archiveAppendix.set(fragment.disambiguateName(""))
            project.pm20Extension.metadataCompilationRegistryByModuleId.getValue(fragment.containingModule.moduleIdentifier)
                .withAll { metadataCompilation ->
                    val metadataFragment = metadataCompilation.fragment
                    if (metadataCompilation is KotlinNativeFragmentMetadataCompilationData) {
                        jar.from(project.files(project.provider {
                            if (metadataFragment in fragment.refinesClosure && metadataFragment.isNativeHostSpecific())
                                project.filesWithUnpackedArchives(metadataCompilation.output.allOutputs, setOf(KLIB_FILE_EXTENSION))
                            else emptyList<Any>()
                        })) { spec -> spec.into(metadataFragment.name) }
                    }
                }
        }

        project.artifacts.add(hostSpecificMetadataElements.name, hostSpecificMetadataJar)
        hostSpecificMetadataElements.dependencies.addAllLater(project.objects.listProperty(Dependency::class.java).apply {
            set(project.provider { fragment.apiElementsConfiguration.allDependencies })
        })
    }
}
