/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package dokkabuild.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Category.DOCUMENTATION
import org.gradle.api.attributes.DocsType.DOCS_TYPE_ATTRIBUTE
import org.gradle.api.attributes.DocsType.SOURCES
import org.gradle.api.attributes.Usage.JAVA_RUNTIME
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.support.serviceOf
import java.io.File


/**
 * Download and unpack Kotlin stdlib JVM source code.
 *
 * @returns the directory containing the unpacked sources.
 */
fun downloadKotlinStdlibJvmSources(project: Project): Provider<File> {
    val kotlinStdlibJvmSources: Configuration by project.configurations.creating {
        description = "kotlin-stdlib JVM source code."
        declarable()
        withDependencies {
            add(project.dependencies.run { create(kotlin("stdlib")) })
        }
    }

    val kotlinStdlibJvmSourcesResolver: Configuration by project.configurations.creating {
        description = "Resolver for ${kotlinStdlibJvmSources.name}."
        resolvable()
        isTransitive = false
        extendsFrom(kotlinStdlibJvmSources)
        attributes {
            attribute(USAGE_ATTRIBUTE, project.objects.named(JAVA_RUNTIME))
            attribute(CATEGORY_ATTRIBUTE, project.objects.named(DOCUMENTATION))
            attribute(DOCS_TYPE_ATTRIBUTE, project.objects.named(SOURCES))
        }
    }

    val downloadKotlinStdlibSources by project.tasks.registering(Sync::class) {
        description = "Download and unpacks kotlin-stdlib JVM source code."
        val archives = project.serviceOf<ArchiveOperations>()
        val unpackedJvmSources = kotlinStdlibJvmSourcesResolver.incoming.artifacts.resolvedArtifacts.map { artifacts ->
            artifacts.map {
                archives.zipTree(it.file)
            }
        }
        from(unpackedJvmSources)
        into(temporaryDir)
    }

    return downloadKotlinStdlibSources.map { it.destinationDir }
}
