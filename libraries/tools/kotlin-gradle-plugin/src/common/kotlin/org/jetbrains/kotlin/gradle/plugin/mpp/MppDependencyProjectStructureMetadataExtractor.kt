/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.dsl.pm20ExtensionOrNull
import org.jetbrains.kotlin.gradle.dsl.topLevelExtensionOrNull

import org.jetbrains.kotlin.project.model.KpmModuleIdentifier
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

sealed class MppDependencyProjectStructureMetadataExtractor {
    abstract fun getProjectStructureMetadata(): KotlinProjectStructureMetadata?

    companion object Factory
}

internal class ProjectMppDependencyProjectStructureMetadataExtractor(
    val moduleIdentifier: KpmModuleIdentifier,
    val dependencyProject: Project
) : MppDependencyProjectStructureMetadataExtractor() {

    override fun getProjectStructureMetadata(): KotlinProjectStructureMetadata? {
        return when {
            dependencyProject.topLevelExtensionOrNull == null -> null
            dependencyProject.pm20ExtensionOrNull != null -> buildProjectStructureMetadata(
                dependencyProject.pm20Extension.modules.single { it.moduleIdentifier == moduleIdentifier }
            )

            else -> dependencyProject.multiplatformExtensionOrNull?.kotlinProjectStructureMetadata
        }
    }
}

internal open class JarMppDependencyProjectStructureMetadataExtractor(
    val primaryArtifactFile: File
) : MppDependencyProjectStructureMetadataExtractor() {

    private fun parseJsonProjectStructureMetadata(input: InputStream) =
        parseKotlinSourceSetMetadataFromJson(input.reader().readText())

    private fun parseXmlProjectStructureMetadata(input: InputStream) =
        parseKotlinSourceSetMetadataFromXml(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input))

    override fun getProjectStructureMetadata(): KotlinProjectStructureMetadata? {
        return ZipFile(primaryArtifactFile).use { zip ->
            val (metadata, parseFunction) =
                zip.getEntry("META-INF/$MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME")?.to(::parseJsonProjectStructureMetadata)
                    ?: zip.getEntry("META-INF/$MULTIPLATFORM_PROJECT_METADATA_FILE_NAME")?.to(::parseXmlProjectStructureMetadata)
                    ?: return null

            zip.getInputStream(metadata).use(parseFunction)
        }
    }
}

internal class IncludedBuildMppDependencyProjectStructureMetadataExtractor(
    private val project: Project,
    dependency: ResolvedComponentResult,
    primaryArtifact: File
) : JarMppDependencyProjectStructureMetadataExtractor(primaryArtifact) {

    private val id: ProjectComponentIdentifier

    init {
        val id = dependency.id
        require(id is ProjectComponentIdentifier) { "dependency should resolve to a project" }
        require(!id.build.isCurrentBuild) { "should be a project from an included build" }
        this.id = id
    }

    override fun getProjectStructureMetadata(): KotlinProjectStructureMetadata? =
        GlobalProjectStructureMetadataStorage.getProjectStructureMetadata(project, id.build.name, id.projectPath)
}
