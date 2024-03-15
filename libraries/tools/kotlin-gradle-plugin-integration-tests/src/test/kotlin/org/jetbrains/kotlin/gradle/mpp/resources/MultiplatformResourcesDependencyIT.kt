/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.resources

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path

@MppGradlePluginTests
@AndroidTestVersions(minVersion = TestVersions.AGP.AGP_73)
@DisplayName("Test consumption of dependencies with multiplatform resources in a regular project")
class MultiplatformResourcesDependencyIT : KGPBaseTest() {

    @DisplayName("Assembling a project with a project dependency and published dependency with multiplatform resources")
    @GradleAndroidTest
    fun test(
        gradleVersion: GradleVersion,
        androidVersion: String,
        providedJdk: JdkVersions.ProvidedJdk,
    ) {
        publishAndPrepareDependencyProjects(
            gradleVersion,
            providedJdk,
            androidVersion,
        ) { localRepoDir ->
            project(
                "multiplatformResources/dependency",
                gradleVersion,
                buildJdk = providedJdk.location,
                localRepoDir = localRepoDir,
            ) {
                includeOtherProjectAsSubmodule(
                    "multiplatformResources/publication",
                    newSubmoduleName = "project",
                    isKts = true,
                )
                buildWithAGPVersion(
                    "assemble",
                    androidVersion = androidVersion,
                    defaultBuildOptions = defaultBuildOptions,
                ) {
                    assertTasksExecuted(
                        ":compileKotlinJvm",
                        ":compileKotlinLinuxX64",
                        ":compileKotlinWasmJs",
                        ":compileKotlinWasmWasi",
                        ":compileKotlinJs",
                        ":compileDebugKotlinAndroid",
                        ":compileReleaseKotlinAndroid",
                        ":project:compileKotlinJvm",
                        ":project:compileKotlinLinuxX64",
                        ":project:compileKotlinWasmJs",
                        ":project:compileKotlinWasmWasi",
                        ":project:compileKotlinJs",
                        ":project:compileDebugKotlinAndroid",
                        ":project:compileReleaseKotlinAndroid",
                    )
                    if (HostManager.host.family.isAppleFamily) {
                        assertTasksExecuted(
                            ":compileKotlinIosArm64",
                            ":compileKotlinIosSimulatorArm64",
                            ":project:compileKotlinIosArm64",
                            ":project:compileKotlinIosSimulatorArm64",
                        )
                    }
                }
            }
        }
    }

    private fun publishAndPrepareDependencyProjects(
        gradleVersion: GradleVersion,
        providedJdk: JdkVersions.ProvidedJdk,
        androidVersion: String,
        withRepositoryRoot: (Path) -> (Unit),
    ) {
        project(
            "multiplatformResources/publication",
            gradleVersion,
            buildJdk = providedJdk.location,
        ) {
            buildWithAGPVersion(
                ":publishAllPublicationsToMavenRepository",
                androidVersion = androidVersion,
                defaultBuildOptions = defaultBuildOptions,
            )
            withRepositoryRoot(projectPath.resolve("build/repo"))
        }
    }

}
