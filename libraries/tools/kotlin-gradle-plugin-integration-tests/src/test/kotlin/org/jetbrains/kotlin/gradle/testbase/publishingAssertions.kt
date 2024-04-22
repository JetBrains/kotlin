/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.testkit.runner.BuildResult
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.test.assertTrue

// Using BuildResult as a scope for tests-DSL.
// It allows to find the method easier in completion in relevant places, and doesn't pollute scope for irrelevant places
@Suppress("UnusedReceiverParameter")
fun BuildResult.assertExpectedMavenRepositoryLayout(
    repositoryRoot: Path,
    repositoryAssertions: MavenRepositoryExpectedLayout.Repository.() -> Unit
): MavenRepositoryExpectedLayout.Repository {
    repositoryRoot.assertExists("Maven repository root")
    return MavenRepositoryExpectedLayout.Repository(repositoryRoot).apply(repositoryAssertions)
}

object MavenRepositoryExpectedLayout {
    data class Repository(val root: Path) {
        /**
         * Checks that a given group exists
         *
         * [group] - comma-separated 'groupId' for Maven publication, e.g. "org.jetbrains.kotlin"
         * (basically, exactly how it is passed to 'group'-property in build.gradle.kts)
         */
        fun assertHasGroup(group: String, groupAsserts: Group.() -> Unit = {}): Group {
            val groupComponents = group.split(".")
            val groupFolder: Path = groupComponents
                .fold(root) { currentPath, component -> currentPath.resolve(component) }
                .assertExists("folder for group ${groupComponents.joinToString(separator = ".")}")

            return Group(groupFolder).apply(groupAsserts)
        }
    }

    data class Group(val root: Path) {
        /**
         * Check that a given module with [moduleId] and [version] exists in [this]-Group
         * [moduleId] is usually a project-name. If you create a test project via a call to something like 'testProject("foo")',
         * then "foo" is exactly the project name you're looking for
         *
         * [version] is a value of 'version'-property in the buildscript
         *
         * Additionally, this method asserts that the module has:
         * - maven-metadata.xml
         * - .pom
         * - .module (Gradle metadata)
         * - <...>-sources.jar
         */
        fun assertHasModule(moduleId: String, version: String, additionalModuleAsserts: Module.() -> Unit = {}): Module {
            val moduleFolder = root.resolve(moduleId).assertExists("folder for module $moduleId")

            moduleFolder.resolve("maven-metadata.xml")
                .assertExists("maven-metadata.xml")
                .assertAlsoHasChecksums()

            val versionFolder = moduleFolder.resolve(version).assertExists("folder for version $version of module $moduleId")

            return Module(moduleId, version, versionFolder).apply {
                assertHasPom()
                assertHasGradleMetadata()
                assertHasSourcesJar()

                additionalModuleAsserts()
            }
        }
    }

    data class Module(val moduleId: String, val version: String, val path: Path) {
        /**
         * Asserts that [this]-Module has an artifact with extension [extension] and [classifier], and name composed via
         * [moduleId] and [version] according to default conventions of Maven repos (see [artifactFileName] in the body)
         */
        fun assertHasArtifact(extension: String, classifier: String? = null, artifactAsserts: Path.() -> Unit = {}): Path {
            val extensionWithDot = if (extension.startsWith(".")) extension else ".$extension"
            val classifierIfAny = if (classifier != null) "-$classifier" else ""
            val artifactFileName = "$moduleId-$version$classifierIfAny$extensionWithDot"
            val artifactPath = path.resolve(artifactFileName)
                .assertExists(".$extension-file")
                .assertAlsoHasChecksums()
            artifactPath.artifactAsserts()
            return artifactPath
        }

        fun assertHasKotlinToolingMetadata(): Path = assertHasArtifact(extension = ".json", classifier = "kotlin-tooling-metadata")
        fun assertHasSourcesJar(): Path = assertHasArtifact(extension = ".jar", classifier = "sources")
        fun assertHasGradleMetadata(): Path = assertHasArtifact(extension = ".module")
        fun assertHasPom(): Path = assertHasArtifact(extension = ".pom")
    }
}


private fun Path.assertExists(shortFileName: String): Path {
    assertTrue(toFile().exists(), "Expected file ($shortFileName) wasn't found. Looked at ${absolute()}")
    return this
}

private fun Path.assertAlsoHasChecksums(checksumsExtensions: List<String> = listOf("md5", "sha1", "sha256", "sha512")): Path {
    checksumsExtensions.forEach { checksumExtension ->
        val checksumFilePath = parent.resolve("$fileName.$checksumExtension")
        checksumFilePath.assertExists("'$checksumExtension' for $fileName")
    }
    return this
}
