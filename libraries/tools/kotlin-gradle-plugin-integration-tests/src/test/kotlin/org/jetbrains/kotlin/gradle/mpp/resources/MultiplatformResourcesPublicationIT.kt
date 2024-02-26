package org.jetbrains.kotlin.gradle.mpp.resources

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.capitalize
import org.jetbrains.kotlin.incremental.testingUtils.assertEqualDirectories
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import java.util.zip.ZipFile

@MppGradlePluginTests
@DisplayName("Test multiplatform resources publication")
class MultiplatformResourcesPublicationIT : KGPBaseTest() {

    @DisplayName("Multiplatform resources publication for jvm target")
    @GradleAndroidTest
    fun testJvmResourcesPublication(
        gradleVersion: GradleVersion,
        androidVersion: String,
        providedJdk: JdkVersions.ProvidedJdk,
    ) {
        testEmbeddedResources(
            gradleVersion,
            androidVersion,
            providedJdk,
            publicationTask = ":publishJvmPublicationToMavenRepository",
            publishedArchive = "build/repo/test/publication-jvm/1.0/publication-jvm-1.0.jar",
            reference = "jvm",
        )
    }

    private fun testEmbeddedResources(
        gradleVersion: GradleVersion,
        androidVersion: String,
        providedJdk: JdkVersions.ProvidedJdk,
        publicationTask: String,
        publishedArchive: String,
        reference: String,
    ) {
        project(
            "multiplatformResources/publication",
            gradleVersion,
            buildJdk = providedJdk.location,
        ) {
            buildWithAGPVersion(
                publicationTask,
                androidVersion,
            )
            compareEmbeddedResources(
                publishedArchive,
                reference
            )
        }
    }

    private fun TestProject.buildWithAGPVersion(
        task: String,
        androidVersion: String,
    ) {
        build(
            task,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion)
        )
    }

    private fun TestProject.compareEmbeddedResources(
        inputZip: String,
        reference: String,
    ) {
        val publishedResources = projectPath.resolve("published/${reference}")
        unzip(
            inputZip = inputZip,
            outputDir = publishedResources,
            filesStartingWith = "embed",
        )
        val referenceResources = projectPath.resolve("reference/$reference")
        assertDirectoryExists(publishedResources)
        assertDirectoryExists(referenceResources)
        assertEqualDirectories(
            publishedResources.toFile(),
            referenceResources.toFile(),
            forgiveExtraFiles = false,
            filter = { !it.name.startsWith(".") }
        )
    }

    private fun TestProject.unzip(
        inputZip: String,
        outputDir: Path,
        filesStartingWith: String,
    ) {
        ZipFile(projectPath.resolve(inputZip).toFile()).use {
            it.entries().asSequence().filter { it.name.startsWith(filesStartingWith) && !it.isDirectory }.forEach { entry ->
                val outputFile = outputDir.resolve(Paths.get(entry.name))
                if (!outputFile.parent.toFile().exists())
                    Files.createDirectories(outputFile.parent)

                it.getInputStream(entry).use { input ->
                    Files.copy(input, outputFile, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

}