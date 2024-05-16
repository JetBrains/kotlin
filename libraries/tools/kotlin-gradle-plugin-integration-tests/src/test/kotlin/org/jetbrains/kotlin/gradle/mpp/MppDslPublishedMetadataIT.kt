/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.test.assertTrue

@MppGradlePluginTests
class MppDslPublishedMetadataIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata("new-mpp-lib-and-app/sample-lib")
    fun testPublishingOnlySupportedNativeTargets(gradleVersion: GradleVersion) {
        nativeProject(
            projectName = "new-mpp-lib-and-app/sample-lib",
            gradleVersion = gradleVersion,
        ) {
            val publishedVariants = MPPNativeTargets.supported
            val nonPublishedVariants = MPPNativeTargets.unsupported

            build("publish") {

                assertTrue(publishedVariants.isNotEmpty())
                publishedVariants.forEach {
                    assertFileInProjectExists("repo/com/example/sample-lib-$it/1.0/sample-lib-$it-1.0.klib")
                }
                nonPublishedVariants.forEach {
                    // check that no artifacts are published for that variant
                    assertFileInProjectNotExists("repo/com/example/sample-lib-$it")
                }

                // but check that the module metadata contains all variants:
                val moduleMetadata = projectPath.resolve("repo/com/example/sample-lib/1.0/sample-lib-1.0.module")
                assertFileContains(moduleMetadata, """"name": "linux64ApiElements-published"""")
                assertFileContains(moduleMetadata, """"name": "mingw64ApiElements-published"""")
                assertFileContains(moduleMetadata, """"name": "macos64ApiElements-published"""")
            }
        }
    }

    @GradleTest
    @TestMetadata("new-mpp-lib-and-app")
    fun testPublishMultimoduleProjectWithMetadata(gradleVersion: GradleVersion) {

        val localRepoDir = defaultLocalRepo(gradleVersion)

        nativeProject(
            projectName = "new-mpp-lib-and-app/sample-external-lib",
            gradleVersion = gradleVersion,
            localRepoDir = localRepoDir,
        ) {
            build("publish")
        }

        nativeProject(
            projectName = "new-mpp-lib-and-app/sample-lib",
            gradleVersion = gradleVersion,
            localRepoDir = localRepoDir,
        ) {
            includeOtherProjectAsSubmodule(
                otherProjectName = "sample-app",
                pathPrefix = "new-mpp-lib-and-app",
                localRepoDir = localRepoDir,
            )

            subProject("sample-app").apply {
                buildGradle.modify {
                    it.replace("'com.example:sample-lib:1.0'", "project(':')")
                }

                buildGradle.append(
                    /* language=groovy */ """
                    |    
                    |apply plugin: 'maven-publish'
                    |
                    |group = "com.exampleapp"
                    |version = "1.0"
                    |
                    |// TODO KT-65528 move publishing config into the actual build.gradle
                    |publishing {
                    |    repositories {
                    |        maven { url "${localRepoDir.invariantSeparatorsPathString}" }
                    |    }
                    |}
                    |
                    |dependencies {
                    |    commonMainApi 'com.external.dependency:external:1.2.3'
                    |}
                    |
                    """.trimMargin()
                )
            }

            settingsGradle.append(
                """
                |include 'sample-app'
                |
                """.trimMargin()
            )

            buildGradle.append(
                /* language=groovy */ """
                |publishing {
                |    publications {
                |        jvm6 {
                |            groupId = "foo"
                |            artifactId = "bar"
                |            version = "42"
                |        }
                |        kotlinMultiplatform {
                |            // KT-29485
                |            artifactId = 'sample-lib-multiplatform'
                |        }
                |    }
                |}
                |
                """.trimMargin()
            )

            build(
                "clean",
                "publish",
                buildOptions = defaultBuildOptions
                    .copy(configurationCache = gradleVersion > GradleVersion.version("8.0")),
            ) {
                assertFileContains(
                    localRepoDir.resolve("com/exampleapp/sample-app-nodejs/1.0/sample-app-nodejs-1.0.pom"),
                    "<groupId>com.example</groupId>",
                    "<artifactId>sample-lib-nodejs</artifactId>",
                    "<version>1.0</version>"
                )
                assertFileContains(
                    localRepoDir.resolve("com/exampleapp/sample-app-jvm8/1.0/sample-app-jvm8-1.0.pom"),
                    "<groupId>foo</groupId>",
                    "<artifactId>bar</artifactId>",
                    "<version>42</version>"
                )
                assertFileContains(
                    localRepoDir.resolve("com/exampleapp/sample-app-jvm8/1.0/sample-app-jvm8-1.0.pom"),
                    "<groupId>com.external.dependency</groupId>",
                    "<artifactId>external-jvm6</artifactId>",
                    "<version>1.2.3</version>"
                )

                // Check that, despite the rewritten POM, the module metadata contains the original dependency:
                val appJvm8ModuleMetadata = localRepoDir.resolve("com/exampleapp/sample-app-jvm8/1.0/sample-app-jvm8-1.0.module")
                assertFileContains(
                    appJvm8ModuleMetadata,
                    """"group":"com.example","module":"sample-lib-multiplatform"""",
                    ignoreWhitespace = true
                )
                assertFileContains(
                    appJvm8ModuleMetadata,
                    """"group":"com.external.dependency","module":"external"""",
                    ignoreWhitespace = true
                )
            }

            // Check that a user can disable rewriting of MPP dependencies in the POMs:
            build("publish", "-Pkotlin.mpp.keepMppDependenciesIntactInPoms=true") {
                assertFileContains(
                    localRepoDir.resolve("com/exampleapp/sample-app-nodejs/1.0/sample-app-nodejs-1.0.pom"),
                    "<groupId>com.example</groupId>",
                    "<artifactId>sample-lib-multiplatform</artifactId>",
                    "<version>1.0</version>"
                )
                assertFileContains(
                    localRepoDir.resolve("com/exampleapp/sample-app-jvm8/1.0/sample-app-jvm8-1.0.pom"),
                    "<groupId>com.example</groupId>",
                    "<artifactId>sample-lib-multiplatform</artifactId>",
                    "<version>1.0</version>"
                )
                assertFileContains(
                    localRepoDir.resolve("com/exampleapp/sample-app-jvm8/1.0/sample-app-jvm8-1.0.pom"),
                    "<groupId>com.external.dependency</groupId>",
                    "<artifactId>external</artifactId>",
                    "<version>1.2.3</version>"
                )
            }
        }
    }
}
