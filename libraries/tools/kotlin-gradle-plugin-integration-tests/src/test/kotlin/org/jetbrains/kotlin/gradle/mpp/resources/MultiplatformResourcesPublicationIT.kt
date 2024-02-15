package org.jetbrains.kotlin.gradle.mpp.resources

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.incremental.testingUtils.assertEqualDirectoriesIgnoringDotFiles
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.name

@MppGradlePluginTests
@AndroidTestVersions(minVersion = TestVersions.AGP.AGP_73)
@DisplayName("Test multiplatform resources publication")
class MultiplatformResourcesPublicationIT : KGPBaseTest() {

    @DisplayName("Multiplatform resources publication for Android target with release build type")
    @GradleAndroidTest
    fun testAndroidReleaseResourcesPublication(
        gradleVersion: GradleVersion,
        androidVersion: String,
        providedJdk: JdkVersions.ProvidedJdk,
    ) {
        project(
            "multiplatformResources/publication",
            gradleVersion,
            buildJdk = providedJdk.location,
        ) {
            buildWithAGPVersion(
                ":publishAndroidReleasePublicationToMavenRepository",
                androidVersion,
                defaultBuildOptions,
            )
            val publishedAarPath = "build/repo/test/publication-android/1.0/publication-android-1.0.aar"
            val classesInAar = projectPath.resolve("classesInAar")
            val classesJar = "classes.jar"
            unzip(
                projectPath.resolve(publishedAarPath),
                classesInAar,
                filesStartingWith = classesJar
            )
            compareEmbeddedResources(
                inputZip = classesInAar.resolve(classesJar),
                reference = reference("androidMain")
            )

            val assetsInAar = projectPath.resolve("assetsInAar")
            unzip(
                projectPath.resolve(publishedAarPath),
                assetsInAar,
                filesStartingWith = "assets"
            )
            assertEqualDirectoriesIgnoringDotFiles(
                assetsInAar.toFile(),
                reference("androidFonts").toFile(),
                forgiveOtherExtraFiles = false,
            )
        }
    }

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
            referenceName = "jvm",
        )
    }

    private fun testEmbeddedResources(
        gradleVersion: GradleVersion,
        androidVersion: String,
        providedJdk: JdkVersions.ProvidedJdk,
        publicationTask: String,
        publishedArchive: String,
        referenceName: String,
    ) {
        project(
            "multiplatformResources/publication",
            gradleVersion,
            buildJdk = providedJdk.location,
        ) {
            buildWithAGPVersion(
                publicationTask,
                androidVersion,
                defaultBuildOptions,
            )
            compareEmbeddedResources(
                projectPath.resolve(publishedArchive),
                reference(referenceName)
            )
        }
    }

    private fun TestProject.reference(
        named: String
    ): Path = projectPath.resolve("reference/$named")

    private fun TestProject.compareEmbeddedResources(
        inputZip: Path,
        reference: Path,
    ) {
        val publishedResources = projectPath.resolve("published/${reference.name}")
        unzip(
            inputZip = inputZip,
            outputDir = publishedResources,
            filesStartingWith = "embed",
        )
        assertDirectoryExists(publishedResources)
        assertDirectoryExists(reference)
        assertEqualDirectoriesIgnoringDotFiles(
            publishedResources.toFile(),
            reference.toFile(),
            forgiveOtherExtraFiles = false,
        )
    }

}
