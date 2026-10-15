/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.mpp

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.internals.parseKotlinSourceSetMetadataFromJson
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.GenerateProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.sources.METADATA_CONFIGURATION_NAME_SUFFIX
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.uklibs.PublishedProject
import org.jetbrains.kotlin.gradle.uklibs.addPublishedProjectToRepositories
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.computeTransformedLibraryChecksum
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.uklibs.metadataTransformationOutputClasspath
import org.jetbrains.kotlin.gradle.uklibs.publish
import org.jetbrains.kotlin.gradle.uklibs.relativeTransformationPathComponents
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.gradle.util.testResolveAllConfigurations
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.utils.addToStdlib.countOccurrencesOf
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@MppGradlePluginTests
class MppMetadataResolutionIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-and-app")
    fun testResolveMppLibDependencyToMetadata(gradleVersion: GradleVersion) {
        project(
            projectName = "new-mpp-lib-and-app/sample-lib",
            gradleVersion = gradleVersion,
            localRepoDir = defaultLocalRepo(gradleVersion),
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
        ) {
            build("publish")
        }

        project(
            projectName = "new-mpp-lib-and-app/sample-app",
            gradleVersion = gradleVersion,
            localRepoDir = defaultLocalRepo(gradleVersion),
        ) {
            buildGradle.replaceText(
                "shouldBeJs = true",
                "shouldBeJs = false",
            )
            buildGradle.append(
                """
                |kotlin.sourceSets {
                |    commonMain {
                |        dependencies {
                |            // add these dependencies to check that they are resolved to metadata
                |            api("com.example:sample-lib:1.0")
                |            compileOnly("com.example:sample-lib:1.0")
                |        }
                |    }
                |}
                """.trimMargin()
            )

            testResolveAllConfigurations { unresolvedConfigurations, buildResult ->
                assertTrue(
                    unresolvedConfigurations.isEmpty(),
                    "Expected no unresolved configurations, but found ${unresolvedConfigurations.size}: $unresolvedConfigurations",
                )

                buildResult.assertOutputContains(">> :commonMainResolvable$METADATA_CONFIGURATION_NAME_SUFFIX --> sample-lib-metadata-1.0.jar")
            }
        }
    }

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-and-app")
    fun testResolveMppProjectDependencyToMetadata(gradleVersion: GradleVersion) {
        project(
            projectName = "new-mpp-lib-and-app/sample-app",
            gradleVersion = gradleVersion,
            // `resolveAllConfigurations` task is not compatible with CC and isolated projects
            buildOptions = defaultBuildOptions.disableIsolatedProjects(),
        ) {

            includeOtherProjectAsSubmodule(
                otherProjectName = "sample-lib",
                pathPrefix = "new-mpp-lib-and-app",
            )

            buildGradle.replaceText(
                """"com.example:sample-lib:1.0"""",
                """project(":sample-lib")""",
            )

            testResolveAllConfigurations { unresolvedConfigurations, buildResult ->
                assertTrue(
                    unresolvedConfigurations.isEmpty(),
                    "Expected no unresolved configurations, but found ${unresolvedConfigurations.size}: $unresolvedConfigurations",
                )

                buildResult.assertOutputContains(">> :commonMainResolvable$METADATA_CONFIGURATION_NAME_SUFFIX --> sample-lib-metadata-1.0.jar")
            }
        }
    }

    @GradleTest
    @TestMetadata(value = "kt-69310-duplicateMetadataLibrariesInClasspath")
    fun testNoDuplicateLibrariesInDiamondStructures(gradleVersion: GradleVersion) {
        project(
            projectName = "kt-69310-duplicateMetadataLibrariesInClasspath",
            gradleVersion = gradleVersion
        ) {
            build(":compileLinuxMainKotlinMetadata") {
                assertOutputDoesNotContain("""KLIB resolver.*The same 'unique_name=.*' found in more than one library""".toRegex())
                val arguments = extractNativeCompilerTaskArguments(":compileLinuxMainKotlinMetadata")
                assertEquals(
                    1,
                    arguments.countOccurrencesOf("kotlinx-kotlinx-coroutines-core-1.8.1-commonMain"),
                    "Unexpected number of kotlinx-kotlinx-coroutines-core-1.8.1-commonMain"
                )
                assertEquals(
                    1,
                    arguments.countOccurrencesOf("kotlinx-kotlinx-coroutines-core-1.8.1-concurrentMain"),
                    "Unexpected number of kotlinx-kotlinx-coroutines-core-1.8.1-concurrentMain"
                )
                assertEquals(
                    1,
                    arguments.countOccurrencesOf("kotlinx-kotlinx-coroutines-core-1.8.1-nativeMain"),
                    "Unexpected number of kotlinx-kotlinx-coroutines-core-1.8.1-concurrentMain"
                )
            }
        }
    }

    @GradleTest
    @GradleTestVersions
    fun testCustomGroupForMppPublicationInTransitiveDependencies(
        gradleVersion: GradleVersion,
    ) {
        val buildOptions = defaultBuildOptions

        fun GradleProject.configureKotlinMultiplatform() {
            buildScriptInjection {
                project.group = "default.group"

                kotlinMultiplatform.jvm()
                kotlinMultiplatform.linuxX64()
            }
        }

        project("base-kotlin-multiplatform-library", gradleVersion) {
            includeOtherProjectAsSubmodule("base-kotlin-multiplatform-library", newSubmoduleName = "lib1") {
                configureKotlinMultiplatform()
                buildScriptInjection {
                    applyMavenPublishPlugin()
                    publishing.publications.withType(MavenPublication::class.java).configureEach {
                        if (it.name == "kotlinMultiplatform") {
                            it.groupId = "custom.group"
                            it.artifactId = "custom-artifact-id"
                        }
                    }
                }

                kotlinSourcesDir("commonMain")
                    .also { it.createDirectories() }
                    .resolve("Lib1.kt")
                    .writeText("interface Lib1")
            }

            includeOtherProjectAsSubmodule("base-kotlin-multiplatform-library", newSubmoduleName = "lib2") {
                configureKotlinMultiplatform()
                buildScriptInjection {
                    kotlinMultiplatform.sourceSets.getByName("commonMain").dependencies {
                        api(project(":lib1"))
                    }
                }

                kotlinSourcesDir("commonMain")
                    .also { it.createDirectories() }
                    .resolve("Lib2.kt")
                    .writeText("interface Lib2 : Lib1")
            }

            includeOtherProjectAsSubmodule("base-kotlin-multiplatform-library", newSubmoduleName = "lib3") {
                configureKotlinMultiplatform()
                buildScriptInjection {
                    kotlinMultiplatform.sourceSets.getByName("commonMain").dependencies {
                        api(project(":lib2"))
                    }
                }

                kotlinSourcesDir("commonMain")
                    .also { it.createDirectories() }
                    .resolve("Lib3.kt")
                    .writeText("class Lib3 : Lib2, Lib1")
            }

            build(":lib3:metadataCommonMainClasses", buildOptions = buildOptions)
        }
    }

    @GradleTest
    @GradleTestVersions(
        maxVersion = TestVersions.Gradle.G_9_0
    )
    fun `KT-77843 - ProjectDependency Project access in pre-PI KGP support`(gradleVersion: GradleVersion) {
        fun projectWithPSM(): TestProject {
            return project("empty", gradleVersion) {
                plugins {
                    kotlin("multiplatform")
                }
                buildScriptInjection {
                    project.group = "foo"
                    project.applyMultiplatform {
                        linuxX64()
                        linuxArm64()
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            }
        }
        val producer = projectWithPSM()
        val consumer = projectWithPSM()
        consumer.include(producer, "producer")
        consumer.buildScriptInjection {
            project.applyMultiplatform {
                sourceSets.commonMain.dependencies {
                    implementation(project(":producer"))
                }
            }
        }
        val taskName = "generateProjectStructureMetadata"
        val path = consumer.providerBuildScriptReturn {
            project.tasks.named(taskName).map {
                it as GenerateProjectStructureMetadata
                it.resultFile
            }
        }.buildAndReturn(
            taskName,
            deriveBuildOptions = {
                defaultBuildOptions.copy(
                    configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED,
                    isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
                )
            },
        )
        assertEquals(
            setOf(org.jetbrains.kotlin.gradle.plugin.mpp.ModuleDependencyIdentifier("foo", "producer")),
            parseKotlinSourceSetMetadataFromJson(path.readText()).sourceSetModuleDependencies["commonMain"],
        )
    }

    @GradleTest
    fun `KT-83917 - dependencySubstitution on target configurations should work in compose-git`(
        gradleVersion: GradleVersion
    ) {
        project("empty", gradleVersion, buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()) {
            plugins {
                kotlin("multiplatform")
            }

            fun KotlinMultiplatformExtension.applyKmpTargets() {
                iosArm64()
                iosSimulatorArm64()
                macosArm64()
                jvm()
                js {
                    browser()
                }
                wasmJs {
                    browser()
                }
            }

            includeOtherProjectAsSubmodule("empty", newSubmoduleName = "androidxCollectionWrapper") {
                buildScriptInjection {
                    project.applyMultiplatform {
                        applyKmpTargets()
                        sourceSets.commonMain.dependencies {
                            api("androidx.collection:collection:1.5.0")
                        }
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            }

            buildScriptInjection {
                project.computeTransformedLibraryChecksum(false)
                project.applyMultiplatform {
                    applyKmpTargets()

                    sourceSets.commonMain.dependencies {
                        api(project(":androidxCollectionWrapper"))
                    }

                    // This is sample code from Compose Multiplatform.git project
                    fun KotlinNativeTarget.substituteForRedirectedPublishedDependencies() {
                        val main = compilations.getByName("main")
                        val test = compilations.getByName("test")

                        listOf(main, test).flatMap {
                            val configurations = it.configurations
                            listOf(
                                configurations.compileDependencyConfiguration,
                                configurations.runtimeDependencyConfiguration,
                                configurations.apiConfiguration,
                                configurations.implementationConfiguration,
                                configurations.runtimeOnlyConfiguration,
                                configurations.compileOnlyConfiguration
                            )
                        }.forEach { c ->
                            c?.incoming?.beforeResolve {
                                c.resolutionStrategy { rs ->
                                    rs.dependencySubstitution { ds ->
                                        ds.substitute(ds.project(":androidxCollectionWrapper"))
                                            .using(ds.module("androidx.collection:collection:1.5.0"))
                                    }
                                }
                            }
                        }
                    }

                    val defaultKonanTargetsPublishedByAndroidx = setOf(
                        KonanTarget.LINUX_X64,
                        KonanTarget.IOS_X64,
                        KonanTarget.IOS_ARM64,
                        KonanTarget.IOS_SIMULATOR_ARM64,
                        KonanTarget.MACOS_X64,
                        KonanTarget.MACOS_ARM64,
                    )
                    targets.all {
                        if (it is KotlinNativeTarget && it.konanTarget in defaultKonanTargetsPublishedByAndroidx) {
                            it.substituteForRedirectedPublishedDependencies()
                        }
                    }

                    sourceSets.commonMain.get().compileSource("""
                        import androidx.collection.*
                        
                        fun a() = floatFloatMapOf()
                        
                        fun b() = longIntMapOf()
                    """.trimIndent())
                }
            }

            assertEquals(
                listOf(
                    listOf("metadata", "commonMain"),
                    listOf("commonMain", "org.jetbrains.kotlin-kotlin-stdlib-${buildOptions.kotlinVersion}-commonMain-.klib"),
                    listOf("commonMain", "androidx.collection-collection-1.5.0-commonMain-.klib"),
                    listOf("commonMain", "androidx.annotation-annotation-1.9.1-commonMain-.klib"),
                ).prettyPrinted,
                metadataTransformationOutputClasspath("commonMain")
                    .relativeTransformationPathComponents().prettyPrinted
            )
        }
    }

    @GradleTest
    fun `KT-84533 - dependency substitution from module to project fails metadata compilation`(
        gradleVersion: GradleVersion,
        @TempDir localRepo: Path,
    ) {
        project("empty", gradleVersion, localRepoDir = localRepo) {
            plugins {
                kotlin("multiplatform").apply(false)
            }
            val localRepoString = localRepo.toString()
            includeOtherProjectAsSubmodule("empty", newSubmoduleName = "producer") {
                buildScriptInjection {
                    project.plugins.apply("maven-publish")
                    publishing.repositories.maven {
                        it.setUrl(localRepoString)
                    }
                    project.group = "test"
                    project.version = "1.0"

                    project.applyMultiplatform {
                        iosArm64()
                        iosSimulatorArm64()
                        jvm()
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            }
            build(":producer:publish")

            buildScriptInjection {
                project.computeTransformedLibraryChecksum(false)
                project.applyMultiplatform {
                    iosArm64()
                    iosSimulatorArm64()
                    jvm()
                    sourceSets.commonMain.dependencies {
                        api("test:producer:1.0")
                    }

                    sourceSets.jvmMain.dependencies {
                        api(project(":producer"))
                    }
                }
            }

            assertEquals(
                listOf(
                    listOf("commonMain", "test-producer-1.0-commonMain-.klib"),
                    listOf("commonMain", "org.jetbrains.kotlin-kotlin-stdlib-${buildOptions.kotlinVersion}-commonMain-.klib"),
                ).prettyPrinted,
                metadataTransformationOutputClasspath(
                    "commonMain",
                ).relativeTransformationPathComponents().prettyPrinted
            )
        }
    }

    @GradleTest
    fun `KT-69571 - dependency substitution from module to project in shared native causes duplicated metadata klibs`(
        gradleVersion: GradleVersion,
        @TempDir localRepo: Path,
    ) {
        project("empty", gradleVersion, localRepoDir = localRepo) {
            plugins {
                kotlin("multiplatform").apply(false)
            }
            val localRepoString = localRepo.toString()
            includeOtherProjectAsSubmodule("empty", newSubmoduleName = "producer") {
                buildScriptInjection {
                    project.plugins.apply("maven-publish")
                    publishing.repositories.maven {
                        it.setUrl(localRepoString)
                    }
                    project.group = "test"
                    project.version = "1.0"

                    project.applyMultiplatform {
                        iosArm64()
                        iosSimulatorArm64()
                        jvm()
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            }
            build(":producer:publish")

            buildScriptInjection {
                project.computeTransformedLibraryChecksum(false)
                project.applyMultiplatform {
                    iosArm64()
                    iosSimulatorArm64()
                    jvm()
                    sourceSets.commonMain.dependencies {
                        api("test:producer:1.0")
                    }

                    sourceSets.nativeMain.dependencies {
                        api(project(":producer"))
                    }
                }
            }

            val flagName = "kotlin.internal.kmp.allowMatchingByRequestedCoordinatesInMetadataTransformations"

            assertEquals(
                listOf(
                    listOf("commonMain", "test-producer-1.0-commonMain-.klib"),
                    listOf("commonMain", "org.jetbrains.kotlin-kotlin-stdlib-${buildOptions.kotlinVersion}-commonMain-.klib"),
                ).prettyPrinted,
                metadataTransformationOutputClasspath("commonMain").relativeTransformationPathComponents().prettyPrinted
            )

            assertEquals(
                listOf(
                    listOf("klib", "producer_iosMain"),
                    listOf("klib", "producer_appleMain"),
                    listOf("klib", "producer_nativeMain"),
                    listOf("commonMain", "test-producer-1.0-commonMain-.klib"),
                    listOf("commonMain", "org.jetbrains.kotlin-kotlin-stdlib-${buildOptions.kotlinVersion}-commonMain-.klib"),
                ).prettyPrinted,
                metadataTransformationOutputClasspath("nativeMain")
                    .relativeTransformationPathComponents().prettyPrinted
            )
        }
    }

    @GradleTest
    fun `KT-89235 path traversal is forbidden via malicious metadata library`(
        gradleVersion: GradleVersion,
        @TempDir tempDir: Path,
    ) {
        val producer = project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.extraProperties.set("kotlin.mpp.enableCInteropCommonization", true)
                project.applyMultiplatform {
                    jvm()
                    linuxArm64()
                    linuxX64()

                    val defFile = project.layout.projectDirectory.file("myCinterop.def")
                    defFile.asFile.writeText(
                        """
                        language = C
                        ---
                        void myCinterop(void);
                        """.trimIndent()
                    )
                    targets.withType(KotlinNativeTarget::class.java).configureEach { target ->
                        target.compilations.getByName("main").cinterops.create("myCinterop") {
                            it.definitionFile.set(defFile)
                        }
                    }

                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    sourceSets.nativeMain.get().compileStubSourceWithSourceSetName()
                    sourceSets.linuxMain.get().compileStubSourceWithSourceSetName()
                }
            }
        }.publish()

        fun assertMaliciousLibraryIsRejected(
            scenario: String,
            rejectedPathComponents: List<String>,
            modifyProjectStructureMetadata: (String) -> String,
        ) {
            val maliciousRepository = tempDir.resolve(scenario).toFile()
            producer.repository.copyRecursively(maliciousRepository)
            val maliciousProducer = producer.copy(repository = maliciousRepository)
            maliciousProducer.rootComponent.modifyProjectStructureMetadataInMetadataJar(modifyProjectStructureMetadata)

            project("empty", gradleVersion) {
                plugins {
                    kotlin("multiplatform")
                }
                addPublishedProjectToRepositories(maliciousProducer)
                buildScriptInjection {
                    project.extraProperties.set("kotlin.mpp.enableCInteropCommonization", true)
                    project.applyMultiplatform {
                        jvm()
                        linuxArm64()
                        linuxX64()

                        sourceSets.commonMain.dependencies {
                            implementation(maliciousProducer.rootCoordinate)
                        }
                        sourceSets.linuxMain.get().compileStubSourceWithSourceSetName()
                    }
                }

                buildAndFail(":compileLinuxMainKotlinMetadata") {
                    // Depending on the order of transformation tasks, either the whole name or its component is rejected first
                    val expectedErrors = rejectedPathComponents.map { "String '$it' is not safe for path component" }
                    assertOutputContainsAny(*expectedErrors.toTypedArray())
                }

                val filesOutsideOfProject = projectPath.parent.toFile().walk()
                    .filter { it.name.contains("pwned") }
                    .toList()
                assertEquals(emptyList(), filesOutsideOfProject, "No files are expected to be written by path traversal")
            }
        }

        assertMaliciousLibraryIsRejected(
            scenario = "sourceSetName",
            rejectedPathComponents = listOf("../../../../pwned"),
            modifyProjectStructureMetadata = { it.replace("\"linuxMain\"", "\"../../../../pwned\"") },
        )
    }

    /**
     * Rewrites `kotlin-project-structure-metadata.json` inside the composite metadata jar of [this] component.
     * Checksums of the rewritten jar are stripped from the Gradle module metadata and the repository.
     */
    private fun PublishedProject.Component.modifyProjectStructureMetadataInMetadataJar(
        modifyProjectStructureMetadata: (String) -> String,
    ) {
        val originalJar = jar.resolveSibling(jar.name + ".original")
        jar.renameTo(originalJar)

        ZipFile(originalJar).use { input ->
            ZipOutputStream(jar.outputStream()).use { output ->
                input.entries().asSequence().forEach { entry ->
                    val content = input.getInputStream(entry).use { it.readBytes() }
                    val newContent = if (entry.name == "META-INF/kotlin-project-structure-metadata.json") {
                        modifyProjectStructureMetadata(content.decodeToString()).encodeToByteArray()
                    } else content
                    output.putNextEntry(ZipEntry(entry.name))
                    output.write(newContent)
                    output.closeEntry()
                }
            }
        }
        originalJar.delete()

        // the checksums are not needed for verifying the issue
        val checksumKeys = listOf("size", "md5", "sha1", "sha256", "sha512")
        // so remove them from disk
        checksumKeys.forEach { jar.resolveSibling("${jar.name}.$it").takeIf { file -> file.exists() }?.delete() }

        // and remove them from module.metadata json file
        val moduleJson = Json.parseToJsonElement(gradleMetadata.readText()).jsonObject
        val variants = moduleJson.getValue("variants").jsonArray.map { variant ->
            val files = variant.jsonObject["files"]?.jsonArray ?: return@map variant
            val filesWithoutChecksums = files.map { file ->
                if (file.jsonObject.getValue("url").jsonPrimitive.content == jar.name) JsonObject(file.jsonObject - checksumKeys.toSet())
                else file
            }
            JsonObject(variant.jsonObject + ("files" to JsonArray(filesWithoutChecksums)))
        }
        gradleMetadata.writeText(Json.encodeToString(JsonObject.serializer(), JsonObject(moduleJson + ("variants" to JsonArray(variants)))))

        // and also module metadata checksums
        checksumKeys.forEach { gradleMetadata.resolveSibling("${gradleMetadata.name}.$it").takeIf { file -> file.exists() }?.delete() }
    }
}
