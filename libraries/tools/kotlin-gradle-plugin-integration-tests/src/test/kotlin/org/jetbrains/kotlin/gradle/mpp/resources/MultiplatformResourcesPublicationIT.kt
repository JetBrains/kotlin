@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.mpp.resources

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.gradle.api.tasks.Copy
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.mpp.publication.ENABLE_KAR_PUBLICATION
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.incremental.testingUtils.assertEqualDirectoriesIgnoringDotFiles
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.name
import kotlin.io.path.outputStream
import kotlin.io.path.writeText
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@MppGradlePluginTests
@DisplayName("Test multiplatform resources publication")
class MultiplatformResourcesPublicationIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @DisplayName("Multiplatform resources publication for Android target with release build type")
    @GradleAndroidTest
    @AndroidTestVersions(maxVersion = TestVersions.AGP.AGP_813)
    fun testAndroidReleaseResourcesPublicationInNewerAgpVersions(
        gradleVersion: GradleVersion,
        androidVersion: String,
        providedJdk: JdkVersions.ProvidedJdk,
    ) = testAndroidReleaseResourcesPublication(
        gradleVersion, androidVersion, providedJdk,
        assertEmbeddedResources = { classesJar ->
            compareEmbeddedResources(
                inputZip = classesJar,
                reference = reference("androidMain")
            )
        },
        assertAssets = { assetsInAar ->
            assertEqualDirectoriesIgnoringDotFiles(
                assetsInAar.toFile(),
                reference("androidFonts").toFile(),
                forgiveOtherExtraFiles = false,
            )
        },
    )

    @DisplayName("Multiplatform resources publication for jvm target")
    @GradleTest
    fun testJvmResourcesPublication(
        gradleVersion: GradleVersion,
    ) {
        testEmbeddedResources(
            gradleVersion,
            androidVersion = null,
            providedJdk = null,
            publicationTask = ":publishJvmPublicationToMavenRepository",
            publishedArchive = "repo/test/publication-jvm/1.0/publication-jvm-1.0.jar",
            referenceName = "jvm",
        )
    }

    @DisplayName("Multiplatform resources are stored unpacked in Kotlin Archive")
    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_14)
    fun testResourcesInKotlinArchive(
        gradleVersion: GradleVersion,
    ) {
        val producer = resourcesProducerProject(
            gradleVersion = gradleVersion,
            enableKarPublication = true,
        )

        producer.build(":publishAllPublicationsToMavenRepository")

        val compressedKar = producer.projectPath.resolve("repo/test/publication/1.0/publication-1.0.kar.xz")
        val kar = producer.projectPath.resolve("publication-1.0.kar")
        compressedKar.toFile().inputStream().buffered().use { fileInput ->
            XZCompressorInputStream(fileInput).use { xzInput ->
                kar.outputStream().buffered().use { output -> xzInput.copyTo(output) }
            }
        }

        ZipFile(kar.toFile()).use { archive ->
            val entries = archive.entries().asSequence().map { it.name }.toSet()
            assertTrue("resources/linuxX64/embed/publication/onlyInLinuxX64" in entries)
            assertTrue("resources/js/embed/publication/onlyInJs" in entries)
            assertTrue("resources/wasmJs/embed/publication/onlyInWasmJs" in entries)
            assertTrue("resources/wasmWasi/embed/publication/onlyInWasmWasi" in entries)
            assertFalse(entries.any { it.startsWith("resources/") && it.endsWith(".zip") })
        }

        val repositoryPath = producer.projectPath.resolve("repo").toString()
        project(
            projectName = "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                freeArgs = defaultBuildOptions.freeArgs + ENABLE_KAR_PUBLICATION,
            ),
        ) {
            addKgpToBuildScriptCompilationClasspath()
            transferDependencyResolutionRepositoriesIntoProjectRepositories()
            settingsBuildScriptInjection {
                settings.dependencyResolutionManagement.repositories.maven { repository ->
                    repository.url = File(repositoryPath).toURI()
                }
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    val publication = project.extraProperties.get(
                        KotlinTargetResourcesPublication.EXTENSION_NAME
                    ) as KotlinTargetResourcesPublication
                    sourceSets.getByName("commonMain").dependencies {
                        implementation("test:publication:1.0")
                    }
                    listOf(linuxX64(), js(), wasmJs(), wasmWasi()).forEach { target ->
                        val resolvedResources = publication.resolveResources(target)
                        project.tasks.register("${target.name}ResolveResources", Copy::class.java) { copy ->
                            copy.from(resolvedResources)
                            copy.into(project.layout.buildDirectory.dir("${target.name}ResolvedResources"))
                        }
                    }
                }
            }

            listOf("linuxX64", "js", "wasmJs", "wasmWasi").forEach { targetName ->
                build(":${targetName}ResolveResources")
                assertEqualDirectoriesIgnoringDotFiles(
                    producer.projectPath.resolve("reference/$targetName").toFile(),
                    projectPath.resolve("build/${targetName}ResolvedResources").toFile(),
                    forgiveOtherExtraFiles = false,
                )
            }
        }
    }

    @DisplayName("Multiplatform resources publication for Native target")
    @GradleTest
    fun testNativeTargetResourcesPublication(
        gradleVersion: GradleVersion,
    ) {
        testEmbeddedResources(
            gradleVersion,
            androidVersion = null,
            providedJdk = null,
            publicationTask = ":publishLinuxX64PublicationToMavenRepository",
            publishedArchive = "repo/test/publication-linuxx64/1.0/publication-linuxx64-1.0-kotlin_resources.kotlin_resources.zip",
            referenceName = "linuxX64",
        )
    }

    @DisplayName("Multiplatform resources publication for wasm js target")
    @GradleTest
    fun testWasmJsTargetResourcesPublication(
        gradleVersion: GradleVersion,
    ) {
        testEmbeddedResources(
            gradleVersion,
            androidVersion = null,
            providedJdk = null,
            publicationTask = ":publishWasmJsPublicationToMavenRepository",
            publishedArchive = "repo/test/publication-wasm-js/1.0/publication-wasm-js-1.0-kotlin_resources.kotlin_resources.zip",
            referenceName = "wasmJs",
        )
    }

    @DisplayName("Multiplatform resources publication for wasm wasi target")
    @GradleTest
    fun testWasmWasiTargetResourcesPublication(
        gradleVersion: GradleVersion,
    ) {
        testEmbeddedResources(
            gradleVersion,
            androidVersion = null,
            providedJdk = null,
            publicationTask = ":publishWasmWasiPublicationToMavenRepository",
            publishedArchive = "repo/test/publication-wasm-wasi/1.0/publication-wasm-wasi-1.0-kotlin_resources.kotlin_resources.zip",
            referenceName = "wasmWasi",
        )
    }

    @DisplayName("Multiplatform resources publication for js target")
    @GradleTest
    fun testJsTargetResourcesPublication(
        gradleVersion: GradleVersion,
    ) {
        testEmbeddedResources(
            gradleVersion,
            androidVersion = null,
            providedJdk = null,
            publicationTask = ":publishJsPublicationToMavenRepository",
            publishedArchive = "repo/test/publication-js/1.0/publication-js-1.0-kotlin_resources.kotlin_resources.zip",
            referenceName = "js",
        )
    }

    @DisplayName("Multiplatform resources publication when a previously non-existent source set with resource is added")
    @GradleTest
    fun testNativeTargetResourcesPublicationWithNewSourceSet(
        gradleVersion: GradleVersion,
    ) {
        val project = resourcesProducerProject(gradleVersion)

        val publishedArchive = project.projectPath.resolve(
            "repo/test/publication-linuxx64/1.0/publication-linuxx64-1.0-kotlin_resources.kotlin_resources.zip"
        )

        project.build(":publishLinuxX64PublicationToMavenRepository")
        project.compareEmbeddedResources(
            publishedArchive,
            project.reference("linuxX64")
        )

        // Add a file to a source set that didn't exist previously
        val linuxMainSourceSet = project.projectPath.resolve("src/linuxMain")
        assertDirectoryDoesNotExist(linuxMainSourceSet)
        val newResource = linuxMainSourceSet.resolve("multiplatformResources/newSourceSetResource")
        assert(newResource.parent.toFile().mkdirs())
        newResource.writeText(newResource.name)

        project.build(":publishLinuxX64PublicationToMavenRepository")
        project.compareEmbeddedResources(
            publishedArchive,
            project.reference("linuxX64WithNewSourceSet")
        )
    }

    private fun testAndroidReleaseResourcesPublication(
        gradleVersion: GradleVersion,
        androidVersion: String,
        providedJdk: JdkVersions.ProvidedJdk,
        assertEmbeddedResources: TestProject.(classesJar: Path) -> (Unit),
        assertAssets: TestProject.(assetsInAar: Path) -> (Unit),
    ) {
        val project = resourcesProducerProject(
            gradleVersion,
            androidVersion = androidVersion,
            providedJdk = providedJdk,
        )

        project.build(
            ":publishAndroidReleasePublicationToMavenRepository",
        )
        val publishedAarPath = "repo/test/publication-android/1.0/publication-android-1.0.aar"
        val classesInAar = project.projectPath.resolve("classesInAar")
        val classesJar = "classes.jar"
        unzip(
            project.projectPath.resolve(publishedAarPath),
            classesInAar,
            filesStartingWith = classesJar
        )
        project.assertEmbeddedResources(classesInAar.resolve(classesJar))

        val assetsInAar = project.projectPath.resolve("assetsInAar")
        unzip(
            project.projectPath.resolve(publishedAarPath),
            assetsInAar,
            filesStartingWith = "assets"
        )
        project.assertAssets(assetsInAar)
    }


    private fun testEmbeddedResources(
        gradleVersion: GradleVersion,
        androidVersion: String?,
        providedJdk: JdkVersions.ProvidedJdk?,
        publicationTask: String,
        publishedArchive: String,
        referenceName: String,
    ) {
        val project = resourcesProducerProject(
            gradleVersion,
            providedJdk,
            androidVersion,
        )
        project.build(publicationTask)
        project.compareEmbeddedResources(
            project.projectPath.resolve(publishedArchive),
            project.reference(referenceName)
        )
    }

    private fun TestProject.reference(
        named: String
    ): Path = projectPath.resolve("reference/$named")

    private fun TestProject.compareEmbeddedResources(
        inputZip: Path,
        reference: Path,
    ) {
        val publishedResources = projectPath.resolve("published/${reference.name}")
        unzipEmbeddedResources(
            inputZip = inputZip,
            outputDir = publishedResources,
        )
        assertDirectoryExists(publishedResources)
        assertDirectoryExists(reference)
        assertEqualDirectoriesIgnoringDotFiles(
            publishedResources.toFile(),
            reference.toFile(),
            forgiveOtherExtraFiles = false,
        )
    }

    private fun unzipEmbeddedResources(
        inputZip: Path,
        outputDir: Path
    ) = unzip(
        inputZip = inputZip,
        outputDir = outputDir,
        filesStartingWith = "embed",
    )

}
