/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.configuration.WarningMode
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Test
import kotlin.test.assertTrue

class VariantAwareDependenciesIT : BaseGradleIT() {
    private val gradleVersion = GradleVersionRequired.FOR_MPP_SUPPORT

    @Test
    fun testJvmKtAppResolvesMppLib() {
        val outerProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        val innerProject = Project("simpleProject")

        with(outerProject) {
            embedProject(innerProject)
            gradleBuildScript(innerProject.projectName).appendText("\ndependencies { implementation rootProject }")
            testResolveAllConfigurations(innerProject.projectName) {
                assertContains(">> :${innerProject.projectName}:runtimeClasspath --> sample-lib-jvm6-1.0.jar")
            }
        }
    }

    @Test
    fun testJsKtAppResolvesMppLib() {
        val outerProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        val innerProject = Project("kotlin2JsInternalTest")

        with(outerProject) {
            embedProject(innerProject)
            gradleBuildScript(innerProject.projectName).appendText("\nrepositories { jcenter() }; dependencies { implementation rootProject }")

            testResolveAllConfigurations(
                subproject = innerProject.projectName,
                options = defaultBuildOptions().copy(warningMode = WarningMode.Summary)
            ) {
                assertContains(">> :${innerProject.projectName}:runtimeClasspath --> sample-lib-nodejs-1.0.jar")
            }

            gradleProperties().appendText(jsCompilerType(KotlinJsCompilerType.IR))

            testResolveAllConfigurations(
                subproject = innerProject.projectName,
                skipSetup = true,
                options = defaultBuildOptions().copy(warningMode = WarningMode.Summary)
            ) {
                assertContains(">> :${innerProject.projectName}:runtimeClasspath --> sample-lib-nodejs-1.0.klib")
            }
        }
    }

    @Test
    fun testMppLibResolvesJvmKtApp() {
        val outerProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        val innerProject = Project("simpleProject")

        with(outerProject) {
            embedProject(innerProject)
            gradleBuildScript().appendText("\ndependencies { jvm6MainImplementation project(':${innerProject.projectName}') }")

            testResolveAllConfigurations(innerProject.projectName)
        }
    }

    @Test
    fun testMppLibResolvesJsKtApp() {
        val outerProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        val innerProject = Project("kotlin2JsInternalTest")

        with(outerProject) {
            embedProject(innerProject)
            gradleBuildScript().appendText("\ndependencies { nodeJsMainImplementation project(':${innerProject.projectName}') }")

            testResolveAllConfigurations(innerProject.projectName, options = defaultBuildOptions().copy(warningMode = WarningMode.Summary))
        }
    }

    @Test
    fun testNonKotlinJvmAppResolvesMppLib() {
        val outerProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        val innerProject = Project("simpleProject").apply {
            setupWorkingDir(false)
            gradleBuildScript().modify {
                it.checkedReplace("id \"org.jetbrains.kotlin.jvm\"", "")
            }

            gradleBuildScript().appendText(
                // In Gradle 5.3+, the variants of a Kotlin MPP can't be disambiguated in a pure Java project's deprecated
                // configurations that don't have a proper 'org.gradle.usage' attribute value, see KT-30378
                "\n" + """
                configurations {
                    configure([compile, runtime, testCompile, testRuntime, getByName('default')]) {
                        canBeResolved = false
                    }
                }
                """.trimIndent()
            )
        }

        with(outerProject) {
            embedProject(innerProject)
            gradleBuildScript(innerProject.projectName).appendText("\ndependencies { implementation rootProject }")

            testResolveAllConfigurations(innerProject.projectName)
        }
    }

    @Test
    fun testJvmKtAppResolvesJvmKtApp() {
        val outerProject = Project("simpleProject", gradleVersion)
        val innerProject = Project("jvmTarget") // cannot use simpleApp again, should be another project

        with(outerProject) {
            embedProject(innerProject)
            gradleBuildScript(innerProject.projectName).appendText("\ndependencies { implementation rootProject }")

            testResolveAllConfigurations(innerProject.projectName)
        }
    }

    @Test
    fun testJsKtAppResolvesJsKtApp() {
        val outerProject = Project("kotlin2JsInternalTest", gradleVersion)
        val innerProject = Project("kotlin2JsNoOutputFileProject")

        with(outerProject) {
            embedProject(innerProject)
            gradleBuildScript(innerProject.projectName).appendText("\ndependencies { implementation rootProject }")

            gradleBuildScript(innerProject.projectName).appendText(
                // Newer Gradle versions fail to resolve the deprecated configurations because of variant-aware resolution ambiguity between
                // the *Elements configuration and its sub-variants (classes, resources)
                "\n" + """
                configurations {
                    configure([compile, runtime, testCompile, testRuntime, getByName('default')]) {
                        canBeResolved = false
                    }
                }
                """.trimIndent()
            )

            testResolveAllConfigurations(innerProject.projectName, options = defaultBuildOptions().copy(warningMode = WarningMode.Summary))
        }
    }

    @Test
    fun testMppResolvesJvmAndJsKtLibs() {
        val outerProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        val innerJvmProject = Project("simpleProject")
        val innerJsProject = Project("kotlin2JsInternalTest")

        with(outerProject) {
            embedProject(innerJvmProject)
            embedProject(innerJsProject)

            gradleBuildScript().appendText(
                "\n" + """
                dependencies {
                    jvm6Implementation project(':${innerJvmProject.projectName}')
                    jvm6TestRuntimeOnly project(':${innerJvmProject.projectName}')
                    nodeJsImplementation project(':${innerJsProject.projectName}')
                    nodeJsTestRuntimeOnly project(':${innerJsProject.projectName}')
                }
            """.trimIndent()
            )

            testResolveAllConfigurations(innerJvmProject.projectName, options = defaultBuildOptions().copy(warningMode = WarningMode.Summary))
        }
    }

    @Test
    fun testJvmKtAppDependsOnMppTestRuntime() {
        val outerProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        val innerProject = Project("simpleProject")

        with(outerProject) {
            embedProject(innerProject)

            gradleBuildScript(innerProject.projectName).appendText(
                "\ndependencies { testImplementation project(path: ':', configuration: 'jvm6RuntimeElements') }"
            )

            testResolveAllConfigurations(innerProject.projectName) {
                assertContains(">> :${innerProject.projectName}:testCompileClasspath --> sample-lib-jvm6-1.0.jar")
                assertContains(">> :${innerProject.projectName}:testRuntimeClasspath --> sample-lib-jvm6-1.0.jar")
            }
        }
    }

    @Test
    fun testKtAppResolvesOldMpp() {
        val outerProject = Project("multiplatformProject")
        val innerJvmProject = Project("simpleProject")
        val innerJsProject = Project("kotlin2JsInternalTest")

        with(outerProject) {
            embedProject(innerJvmProject)
            embedProject(innerJsProject)

            listOf(innerJvmProject to ":libJvm", innerJsProject to ":libJs").forEach { (project, dependency) ->
                gradleBuildScript(project.projectName).appendText(
                    "\n" + """
                        dependencies {
                            implementation project('$dependency')
                            implementation project(':lib')
                        }
                    """.trimIndent()
                )

                testResolveAllConfigurations(
                    project.projectName,
                    options = defaultBuildOptions().copy(warningMode = WarningMode.Summary)
                )
            }
        }
    }

    @Test
    fun testResolvesOldKotlinArtifactsPublishedWithMetadata() = with(Project("multiplatformProject", gradleVersion)) {
        setupWorkingDir()
        gradleBuildScript().appendText(
            "\n" + """
                configure([project(':lib'), project(':libJvm'), project(':libJs')]) {
                    group 'com.example.oldmpp'
                    version '1.0'
                    apply plugin: 'maven-publish'
                    afterEvaluate {
                        publishing {
                            repositories { maven { url "file://${'$'}{rootDir.absolutePath.replace('\\', '/')}/repo" } }
                            publications { kotlin(MavenPublication) { from(components.java) } }
                        }
                    }
                }
            """.trimIndent()
        )
        build("publish") { assertSuccessful() }

        gradleBuildScript().modify {
            it.replace("'com.example.oldmpp'", "'com.example.consumer'") +
                    "\nsubprojects { repositories { maven { url \"file://${'$'}{rootDir.absolutePath.replace('\\\\', '/')}/repo\" } } }"
        }

        gradleBuildScript("lib").appendText("\ndependencies { implementation 'com.example.oldmpp:lib:1.0' }")
        testResolveAllConfigurations("lib")

        gradleBuildScript("libJvm").appendText("\ndependencies { implementation 'com.example.oldmpp:libJvm:1.0' }")
        testResolveAllConfigurations("libJvm")

        gradleBuildScript("libJs").appendText("\ndependencies { implementation 'com.example.oldmpp:libJs:1.0' }")
        testResolveAllConfigurations("libJs")

        embedProject(Project("sample-lib", directoryPrefix = "new-mpp-lib-and-app"))
        gradleBuildScript("sample-lib").appendText(
            "\n" + """
            dependencies {
                commonMainApi 'com.example.oldmpp:lib:1.0'
                jvm6MainApi 'com.example.oldmpp:libJvm:1.0'
                nodeJsMainApi 'com.example.oldmpp:libJs:1.0'
            }
        """.trimIndent()
        )
        testResolveAllConfigurations("sample-lib")
    }

    @Test
    fun testJvmWithJavaProjectCanBeResolvedInAllConfigurations() =
        with(Project("new-mpp-jvm-with-java-multi-module", GradleVersionRequired.FOR_MPP_SUPPORT)) {
            testResolveAllConfigurations("app")
        }

    @Test
    fun testConfigurationsWithNoExplicitUsageResolveRuntime() =
    // Starting with Gradle 5.0, plain Maven dependencies are represented as two variants, and resolving them to the API one leads
    // to transitive dependencies left out of the resolution results. We need to ensure that our attributes schema does not lead to the API
        // variants chosen over the runtime ones when resolving a configuration with no required Usage:
        with(Project("simpleProject")) {
            setupWorkingDir()
            gradleBuildScript().appendText(
                "\n" + """
                    dependencies { implementation 'org.jetbrains.kotlin:kotlin-compiler-embeddable' }

                    configurations {
                        customConfiguration.extendsFrom implementation
                        customConfiguration.canBeResolved(true)
                    }
                """.trimIndent()
            )

            testResolveAllConfigurations {
                assertContains(">> :customConfiguration --> kotlin-compiler-embeddable-${defaultBuildOptions().kotlinVersion}.jar")

                // Check that the transitive dependencies with 'runtime' scope are also available:
                assertContains(">> :customConfiguration --> kotlin-script-runtime-${defaultBuildOptions().kotlinVersion}.jar")
            }
        }

    @Test
    fun testCompileAndRuntimeResolutionOfElementsConfigurations() =
        with(Project("sample-app", gradleVersion, "new-mpp-lib-and-app")) {
            val libProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
            embedProject(libProject)
            gradleBuildScript().modify {
                it.replace("'com.example:sample-lib:1.0'", "project('${libProject.projectName}')")
            }

            listOf("jvm6" to "Classpath", "nodeJs" to "Classpath").forEach { (target, suffix) ->
                build("dependencyInsight", "--configuration", "${target}Compile$suffix", "--dependency", "sample-lib") {
                    assertSuccessful()
                    assertContains("variant \"${target}ApiElements\" [")
                }

                if (suffix == "Classpath") {
                    build("dependencyInsight", "--configuration", "${target}Runtime$suffix", "--dependency", "sample-lib") {
                        assertSuccessful()
                        assertContains("variant \"${target}RuntimeElements\" [")
                    }
                }
            }
        }

    @Test
    fun testResolveDependencyOnMppInCustomConfiguration() = with(Project("simpleProject", GradleVersionRequired.FOR_MPP_SUPPORT)) {
        setupWorkingDir()

        gradleBuildScript().appendText(
            "\n" + """
            configurations.create("custom")
            dependencies { custom("org.jetbrains.kotlinx:atomicfu:0.15.2") }
            tasks.register("resolveCustom") { doLast { println("###" + configurations.custom.toList()) } }
            """.trimIndent()
        )

        build("resolveCustom") {
            assertSuccessful()
            val printedLine = output.lines().single { "###[" in it }.substringAfter("###")
            val items = printedLine.removeSurrounding("[", "]").split(", ")
            assertTrue(items.toString()) { items.any { "atomicfu-jvm" in it } }
        }
    }

    @Test
    fun testResolveCompatibilityMetadataVariantWithNative() = with(Project("native-libraries")) {
        setupWorkingDir()
        val nestedProjectName = "nested"
        embedProject(Project("native-libraries"), nestedProjectName)

        projectDir.resolve("gradle.properties").appendText(
            "\n" + """
                kotlin.mpp.enableGranularSourceSetsMetadata=true
                kotlin.mpp.enableCompatibilityMetadataVariant=true
            """
        )

        val resolveConfigurationTaskName = "resolveConfiguration"
        val marker = "###=>"

        gradleBuildScript().appendText(
            "\n" + """
                dependencies { "commonMainImplementation"(project(":$nestedProjectName")) }
                tasks.create("$resolveConfigurationTaskName") {
                    doFirst {
                        println("$marker" + configurations.getByName("metadataCompileClasspath").toList())
                    }
                }
            """
        )

        build(resolveConfigurationTaskName) {
            assertSuccessful()
            val output = output.lines().single { marker in it }.substringAfter(marker).removeSurrounding("[", "]").split(",")
            assertTrue { output.any { "$nestedProjectName-1.0.jar" in it } }
        }
    }

}

internal fun BaseGradleIT.Project.embedProject(other: BaseGradleIT.Project, renameTo: String? = null) {
    setupWorkingDir()
    other.setupWorkingDir(false)
    val tempDir = createTempDir(if (isWindows) "" else "BaseGradleIT")
    val embeddedModuleName = renameTo ?: other.projectName
    try {
        other.projectDir.copyRecursively(tempDir)
        tempDir.copyRecursively(projectDir.resolve(embeddedModuleName))
    } finally {
        check(tempDir.deleteRecursively())
    }
    if (projectName == other.projectName) {
        val embeddedModuleDir = projectDir.resolve(embeddedModuleName)
        embeddedModuleDir.walk().forEach {
            if (it.name.contains("build.gradle")) {
                it.modify { string ->
                    string.lines().dropLast(5).joinToString(separator = "\n")
                }
            }
        }
    }
    testCase.apply {
        gradleSettingsScript().appendText("\ninclude(\"$embeddedModuleName\")")

        val embeddedBuildScript = gradleBuildScript(embeddedModuleName)
        if (embeddedBuildScript.extension == "kts") {
            embeddedBuildScript.modify { it.replace(".version(\"$PLUGIN_MARKER_VERSION_PLACEHOLDER\")", "") }
        }
    }
}