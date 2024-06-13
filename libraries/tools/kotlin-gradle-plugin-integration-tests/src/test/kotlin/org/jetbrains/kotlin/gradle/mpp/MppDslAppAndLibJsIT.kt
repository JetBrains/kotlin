/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.ProjectLocalConfigurations
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.name

@MppGradlePluginTests
class MppDslAppAndLibJsIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata(value = "both-js-lib-and-app")
    fun testLibAndAppJsIr(gradleVersion: GradleVersion) {
        doTestLibAndAppJsBothCompilers(
            libProjectPath = "both-js-lib-and-app/sample-lib",
            appProjectPath = "both-js-lib-and-app/sample-app",
            gradleVersion = gradleVersion,
        )
    }

    @GradleTest
    @TestMetadata(value = "both-js-lib-and-app")
    fun testLibAndAppWithGradleKotlinDslJsIr(gradleVersion: GradleVersion) {
        doTestLibAndAppJsBothCompilers(
            libProjectPath = "both-js-lib-and-app/sample-lib-gradle-kotlin-dsl",
            appProjectPath = "both-js-lib-and-app/sample-app-gradle-kotlin-dsl",
            gradleVersion = gradleVersion,
        )
    }

    private fun doTestLibAndAppJsBothCompilers(
        libProjectPath: String,
        appProjectPath: String,
        gradleVersion: GradleVersion,
    ) {
        val localRepoDir = defaultLocalRepo(gradleVersion)

        val libProject = project(
            projectName = libProjectPath,
            gradleVersion = gradleVersion,
            localRepoDir = localRepoDir,
        ) {
            build("publish") {
                assertTasksAreNotInTaskGraph(":compileCommonMainKotlinMetadata")
                assertTasksExecuted(
                    ":compileKotlinNodeJs",
                    ":allMetadataJar",
                )

                val groupDir = localRepoDir.resolve("com/example")
                assertDirectoryExists(groupDir)

                val jsKlib = groupDir.resolve("sample-lib-nodejs/1.0/sample-lib-nodejs-1.0.klib")
                val jsPom = groupDir.resolve("sample-lib-nodejs/1.0/sample-lib-nodejs-1.0.pom")
                val metadataJar = groupDir.resolve("sample-lib/1.0/sample-lib-1.0.jar")
                val gradleModule = groupDir.resolve("sample-lib/1.0/sample-lib-1.0.module")

                assertFileExists(jsKlib)
                assertFileExists(jsPom)
                assertFileExists(metadataJar)
                assertFileExists(gradleModule)

                assertFileDoesNotContain(gradleModule, ProjectLocalConfigurations.ATTRIBUTE.name)

                assertFileContains(jsPom, "<name>Sample MPP library</name>")
                assertFileDoesNotContain(
                    jsPom,
                    "<groupId>Kotlin/Native</groupId>",
                    message = "$jsPom should not contain standard K/N libraries as dependencies.",
                )
            }
        }

        project(
            projectName = appProjectPath,
            gradleVersion = gradleVersion,
            localRepoDir = localRepoDir,
        ) {
            build("assemble") {
                assertTasksExecuted(":compileKotlinNodeJs")
            }

            // Now run again with a project dependency instead of a module one:
            val libSubprojectName = libProject.projectPath.name
            includeOtherProjectAsSubmodule(
                otherProjectName = libProjectPath,
                newSubmoduleName = libSubprojectName,
                isKts = settingsGradleKts.exists(),
            )
            // Delete the lib local repo, to ensure that Gradle uses the subproject
            localRepoDir.deleteRecursively()

            val buildScript = if (buildGradle.exists()) buildGradle else buildGradleKts
            buildScript.modify {
                it.replace(""""com.example:sample-lib:1.0"""", """project(":${libSubprojectName}")""")
            }

            build(
                "assemble",
                "--rerun-tasks",
            ) {
                assertTasksExecuted(":${libSubprojectName}:assemble")
                assertTasksExecuted(":compileKotlinNodeJs")
            }
        }
    }
}
