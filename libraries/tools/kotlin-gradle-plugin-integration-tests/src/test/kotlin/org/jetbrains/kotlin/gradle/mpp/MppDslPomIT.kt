/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import kotlin.io.path.pathString

@MppGradlePluginTests
class MppDslPomIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata(value = "kt-27059-pom-rewriting")
    fun testPomRewritingInSinglePlatformProject(gradleVersion: GradleVersion) {

        val localRepoDir = defaultLocalRepo(gradleVersion)

        project(
            projectName = "kt-27059-pom-rewriting",
            gradleVersion = gradleVersion,
            localRepoDir = localRepoDir,
        ) {
            val repoGroupDir = localRepoDir.resolve("com/example").pathString

            build(":mpp-lib:publish") {
                assertDirectoryInProjectExists("$repoGroupDir/mpp-lib")
                assertDirectoryInProjectExists("$repoGroupDir/mpp-lib-myjvm")
            }

            fun doTestPomRewriting(mppProjectDependency: Boolean, keepPomIntact: Boolean = false) {

                val buildArgs = buildList {
                    add("clean")
                    add(":jvm-app:publish")
                    add(":js-app:publish")
                    add("-PmppProjectDependency=$mppProjectDependency")
                    add("-Pkotlin.mpp.keepMppDependenciesIntactInPoms=$keepPomIntact")
                }.toTypedArray()

                build(*buildArgs) {
                    assertTasksExecuted(
                        ":jvm-app:publishMainPublicationToLocalRepoRepository",
                        ":js-app:publishMavenPublicationToLocalRepoRepository",
                    )

                    val jvmPom = "$repoGroupDir/jvm-app/1.0/jvm-app-1.0.pom"
                    val jsPom = "$repoGroupDir/js-app/1.0/js-app-1.0.pom"

                    if (keepPomIntact) {
                        // The JVM POM should contain the original dependency on 'mpp-lib'
                        assertFileInProjectContains(
                            jvmPom,
                            "<groupId>com.example</groupId><artifactId>mpp-lib</artifactId><version>1.0</version><scope>compile</scope>",
                            ignoreWhitespace = true,
                        )

                        // The JS POM should contain the original dependency on 'mpp-lib'
                        assertFileInProjectContains(
                            jsPom,
                            "<groupId>com.example</groupId><artifactId>mpp-lib</artifactId><version>1.0</version><scope>compile</scope>",
                            ignoreWhitespace = true,
                        )
                    } else {
                        // The JVM POM should contain the dependency on 'mpp-lib' rewritten as 'mpp-lib-myjvm'
                        assertFileInProjectContains(
                            jvmPom,
                            "<groupId>com.example</groupId><artifactId>mpp-lib-myjvm</artifactId><version>1.0</version><scope>compile</scope>",
                            ignoreWhitespace = true,
                        )
                    }
                }
            }

            doTestPomRewriting(mppProjectDependency = false)
            doTestPomRewriting(mppProjectDependency = true)

            // Also check that the flag for keeping POMs intact works:
            doTestPomRewriting(mppProjectDependency = false, keepPomIntact = true)
        }
    }
}
