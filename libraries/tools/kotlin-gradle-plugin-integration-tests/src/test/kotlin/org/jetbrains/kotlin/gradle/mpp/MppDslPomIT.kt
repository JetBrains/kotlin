/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import groovy.util.Node
import groovy.util.NodeList
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.pathString
import kotlin.test.assertEquals

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
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
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
                    val jsPom = "$repoGroupDir/js-app-js/1.0/js-app-js-1.0.pom"

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

    @GradleTest
    fun `POM dependencies rewriter tracks changes of dependent project coordinates`(
        gradleVersion: GradleVersion
    ) {
        fun GradleProjectBuildScriptInjectionContext.configureKmpProject() {
            applyMavenPublishPlugin()
            project.version = "1.0.0"
            project.group = "sample"

            kotlinMultiplatform.apply {
                jvm()
                linuxX64()
            }
        }

        project("base-kotlin-multiplatform-library", gradleVersion) {
            includeOtherProjectAsSubmodule("base-kotlin-multiplatform-library", newSubmoduleName = "libA") {
                buildScriptInjection {
                    configureKmpProject()
                }
            }

            includeOtherProjectAsSubmodule("base-kotlin-multiplatform-library", newSubmoduleName = "libB") {
                buildScriptInjection {
                    configureKmpProject()
                }
            }

            buildScriptInjection {
                configureKmpProject()
                kotlinMultiplatform.apply {
                    sourceSets.commonMain.dependencies {
                        // add dependency only to lib A
                        api(project(":libA"))
                    }
                }
            }

            build(":generatePomFileForJvmPublication") {
                assertTasksExecuted(
                    ":libA:exportTargetPublicationCoordinatesForJvmApiElements",
                    ":libA:exportTargetPublicationCoordinatesForJvmRuntimeElements",
                    ":generatePomFileForJvmPublication"
                )
                assertTasksAreNotInTaskGraph(
                    ":libA:exportTargetPublicationCoordinatesForLinuxX64ApiElements",
                    ":libB:exportTargetPublicationCoordinatesForJvmApiElements",
                    ":libB:exportTargetPublicationCoordinatesForJvmRuntimeElements",
                )
            }

            /** up-to-date check is not working because [org.gradle.api.publish.maven.tasks.GenerateMavenPom]
            *  is annotated with [org.gradle.api.tasks.UntrackedTask] before Gradle 9.8.
            *  Since Gradle 9.8 the task is tracked, but input tracking is disabled when a `withXml` action is registered,
            *  which is the case for the KMP POM dependencies rewriter. */
            build(":generatePomFileForJvmPublication") {
                assertTasksExecuted(":generatePomFileForJvmPublication")
            }
        }
    }

    @GradleTest
    fun `root KMP publication POM is up-to-date and tracks dependency changes`(
        gradleVersion: GradleVersion
    ) {
        fun GradleProjectBuildScriptInjectionContext.configureKmpProject() {
            applyMavenPublishPlugin()
            project.group = "sample"

            kotlinMultiplatform.apply {
                jvm()
                linuxX64()
            }
        }

        project("base-kotlin-multiplatform-library", gradleVersion) {
            includeOtherProjectAsSubmodule("base-kotlin-multiplatform-library", newSubmoduleName = "libA") {
                buildScriptInjection {
                    configureKmpProject()
                    project.version = project.providers.gradleProperty("libAVersion").orElse("1.0.0").get()
                }
            }

            buildScriptInjection {
                configureKmpProject()
                project.version = "1.0.0"
                val externalVersion = project.providers.gradleProperty("externalVersion").orElse("1.0.0").get()
                kotlinMultiplatform.apply {
                    sourceSets.commonMain.dependencies {
                        api(project(":libA"))
                        api("org.example:external:$externalVersion")
                    }
                }
            }

            val rootPomTask = ":generatePomFileForKotlinMultiplatformPublication"
            fun assertRootPomDependencies(expectedVersions: Map<String, String>) {
                val pom = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(projectPath.resolve("build/publications/kotlinMultiplatform/pom-default.xml").toFile())
                val dependencies = pom.getElementsByTagName("dependency")
                val actualVersions = (0 until dependencies.length).associate { index ->
                    val dependency = dependencies.item(index) as Element
                    fun childText(name: String) = dependency.getElementsByTagName(name).item(0).textContent
                    childText("artifactId") to childText("version")
                }
                // kotlin-stdlib is also present in the POM, it is not relevant for this test
                assertEquals(expectedVersions, actualVersions.filterKeys { it in expectedVersions.keys })
            }

            build(rootPomTask) {
                assertTasksExecuted(rootPomTask)
                assertRootPomDependencies(mapOf("libA" to "1.0.0", "external" to "1.0.0"))
            }

            build(rootPomTask) {
                // Since Gradle 9.8 GenerateMavenPom participates in up-to-date checks
                if (gradleVersion >= GradleVersion.version(TestVersions.Gradle.G_9_8)) {
                    assertTasksUpToDate(rootPomTask)
                } else {
                    assertTasksExecuted(rootPomTask)
                }
            }

            build(rootPomTask, "-PexternalVersion=2.0.0") {
                assertTasksExecuted(rootPomTask)
                assertRootPomDependencies(mapOf("libA" to "1.0.0", "external" to "2.0.0"))
            }

            build(rootPomTask, "-PexternalVersion=2.0.0", "-PlibAVersion=2.0.0") {
                assertTasksExecuted(rootPomTask)
                assertRootPomDependencies(mapOf("libA" to "2.0.0", "external" to "2.0.0"))
            }
        }
    }

    @GradleTest
    fun `KT-75512 user withXml should be invoked after KMP POM rewriter`(
        gradleVersion: GradleVersion
    ) {
        fun GradleProjectBuildScriptInjectionContext.configureKmpProject() {
            applyMavenPublishPlugin()
            project.version = "1.0.0"
            project.group = "sample"

            kotlinMultiplatform.apply {
                jvm()
                linuxX64()
            }
        }

        project("base-kotlin-multiplatform-library", gradleVersion) {
            includeOtherProjectAsSubmodule("base-kotlin-multiplatform-library", newSubmoduleName = "utils") {
                buildScriptInjection {
                    project.group = "sample"
                    configureKmpProject()
                }
            }

            buildScriptInjection {
                configureKmpProject()
                kotlinMultiplatform.apply {
                    sourceSets.commonMain.dependencies {
                        api(project(":utils"))
                    }
                    targets.configureEach {
                        it.mavenPublication {
                            // Append artifact ID string i.e.
                            // <dependency>
                            //  <artifactId>OLD-$MyArtifactId</artifactId>
                            //  ...
                            //  </dependency>
                            // If pom rewriter did its job, it should contain target-specific artifact ID
                            pom.withXml { xmlProvider ->
                                val root = xmlProvider.asNode()
                                val dependenciesNode = (root.get("dependencies") as NodeList).filterIsInstance<Node>().single()
                                val dependencyNodes = (dependenciesNode.get("dependency") as? NodeList).orEmpty().filterIsInstance<Node>()
                                fun Node.updateChildNodeByName(name: String, map: (String) -> String) {
                                    val childNode = (get(name) as NodeList).single() as Node
                                    val newValue = map(childNode.value().toString())
                                    childNode.setValue(newValue)
                                }
                                for (dependencyNode in dependencyNodes) {
                                    dependencyNode.updateChildNodeByName("artifactId") { "$it-MyArtifactId" }
                                }
                            }
                        }
                    }
                }
            }

            val tasks = arrayOf(
                ":generatePomFileForJvmPublication",
                ":generatePomFileForLinuxX64Publication"
            )

            fun assertTargetPublicationPomIsCorrect(targetName: String) {
                assertFileInProjectContains(
                    "build/publications/$targetName/pom-default.xml",
                    "<artifactId>utils-${targetName.toLowerCaseAsciiOnly()}-MyArtifactId</artifactId>"
                )
            }

            build(*tasks) {
                assertTasksExecuted(*tasks)
                assertTargetPublicationPomIsCorrect("jvm")
                assertTargetPublicationPomIsCorrect("linuxX64")
            }
        }
    }
}

