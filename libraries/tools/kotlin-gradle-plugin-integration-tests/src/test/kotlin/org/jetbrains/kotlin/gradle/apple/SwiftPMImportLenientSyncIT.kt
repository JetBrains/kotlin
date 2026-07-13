/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.ConvertSyntheticSwiftPMImportProjectIntoDefFile
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FetchSyntheticImportProjectPackages
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.DumpXcodeBuildArgs
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SerializeSwiftPMDependenciesMetadataForLockFiles
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.writeText

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@GradleTestVersions(
    minVersion = TestVersions.Gradle.G_8_0
)
@DisplayName("SwiftPM import does not fail IDE sync when the pipeline fails (KT-85468)")
@SwiftPMImportGradlePluginTests
class SwiftPMImportLenientSyncIT : KGPBaseTest() {

    private val ideaSyncBuildOptions
        get() = defaultBuildOptions.copy(freeArgs = listOf("-Didea.sync.active=true"))

    private val ideaImportTask = ":prepareKotlinIdeaImport"

    private val generateTask = ":${GenerateSyntheticLinkageImportProject.syntheticImportProjectGenerationTaskName}"
    private val serializeTask = ":${SerializeSwiftPMDependenciesMetadataForLockFiles.TASK_NAME}"
    private val umbrellaFetchTask = ":${FetchSyntheticImportProjectPackages.fetchUmbrellaPackageTaskName("default")}"
    private fun convertTask(sdk: String) = ":${ConvertSyntheticSwiftPMImportProjectIntoDefFile.TASK_NAME}$sdk"
    private fun dumpTask(sdk: String) = ":${DumpXcodeBuildArgs.TASK_NAME}$sdk"

    @DisplayName("Failing 'swift package resolve' fails a normal build but only warns during IDE sync")
    @GradleTest
    fun `resolve failure is lenient during IDE sync`(version: GradleVersion) {
        project("empty", version) {
            val cacheDirFile = projectPath.resolve("customXcodePackageCache").toFile()
            initSwiftPmProject(cacheDirFile) {
                swiftPMDependencies {
                    swiftPackage(
                        url = "https://example.invalid/NonExistentPackage.git",
                        version = "1.0.0",
                        products = listOf("NonExistent"),
                    )
                }
            }

            /* Normal build: pipeline failure aborts the build */
            buildAndFail(ideaImportTask) {
                assertTasksExecuted(generateTask, serializeTask)
                assertTasksFailed(umbrellaFetchTask)
            }

            /* IDE sync: pipeline failure is downgraded to a warning, import succeeds */
            build(ideaImportTask, buildOptions = ideaSyncBuildOptions) {
                assertTasksExecuted(umbrellaFetchTask)
                assertOutputContains("Warning: Failed to resolve SwiftPM packages")
            }

            /* Lenient failure is not up-to-date: it re-runs and warns again on the next sync */
            build(ideaImportTask, buildOptions = ideaSyncBuildOptions) {
                assertTasksExecuted(umbrellaFetchTask)
                assertOutputContains("Warning: Failed to resolve SwiftPM packages")
            }
        }
    }

    @DisplayName("Failing xcodebuild fails a normal build but only warns during IDE sync")
    @GradleTest
    fun `xcodebuild failure is lenient during IDE sync`(version: GradleVersion) {
        project("empty", version) {
            val cacheDirFile = projectPath.resolve("customXcodePackageCache").toFile()
            val localPackageDir = projectPath.resolve("localBrokenPackage")
            val packageName = "BrokenPackage"

            // Create a local SwiftPM package with valid manifest but broken source code.
            // swift package resolve will succeed (only resolves the graph), but xcodebuild
            // will fail when trying to compile the broken source.
            createLocalSwiftPackage(localPackageDir, packageName)
            localPackageDir.resolve("Sources/$packageName/$packageName.swift").writeText(
                """
                    import Foundation

                    // Intentionally broken: undeclared identifier
                    @objc public class BrokenClass: NSObject {
                        @objc public func broken() -> String {
                            return thisDoesNotExist()
                        }
                    }
                """.trimIndent()
            )

            initSwiftPmProject(
                cacheDirFile,
                nativeTargets = {
                    listOf(iosArm64())
                },
            ) {
                swiftPMDependencies {
                    localSwiftPackage(
                        directory = project.layout.projectDirectory.dir("localBrokenPackage"),
                        products = listOf(packageName),
                    )
                }
            }

            /* Normal build: xcodebuild failure aborts the build */
            buildAndFail(ideaImportTask) {
                assertTasksExecuted(umbrellaFetchTask, generateTask, serializeTask)
                assertTasksFailed(dumpTask("Iphoneos"))
                assertOutputContains("xcodebuild")
            }

            /* IDE sync: xcodebuild failure is downgraded to a warning, import succeeds */
            build(ideaImportTask, buildOptions = ideaSyncBuildOptions) {
                assertTasksExecuted(dumpTask("Iphoneos"))
                assertOutputContains("Warning: Failed to dump xcodebuild arguments")
            }

            /* Lenient failure is not up-to-date: it re-runs and warns again on the next sync */
            build(ideaImportTask, buildOptions = ideaSyncBuildOptions) {
                assertTasksExecuted(dumpTask("Iphoneos"))
                assertOutputContains("Warning: Failed to dump xcodebuild arguments")
            }
        }
    }
}
