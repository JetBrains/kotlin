/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.test

import androidx.testutils.gradle.ProjectSetupRule
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

@RunWith(JUnit4::class)
class CompilerPluginRuntimeVersionCheckTest {
    companion object {
        private const val MAIN_DIR = "app/src/main"
        private const val SOURCE_DIR = "$MAIN_DIR/java/androidx/compose/compiler/test"
    }

    @get:Rule
    val projectSetup = ProjectSetupRule()

    private lateinit var gradleRunner: GradleRunner

    private lateinit var projectRoot: File

    private val compilerPluginVersion by lazy {
        val metadataFile = File(projectSetup.props.tipOfTreeMavenRepoPath).resolve(
            "androidx/compose/compiler/compiler/maven-metadata.xml"
        )
        check(metadataFile.exists()) {
            "Cannot find compose metadata file in ${metadataFile.absolutePath}"
        }
        check(metadataFile.isFile) {
            "Metadata file should be a file but it is not."
        }
        val xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(metadataFile)
        val latestVersionNode = XPathFactory.newInstance().newXPath()
            .compile("/metadata/versioning/latest").evaluate(
                xmlDoc, XPathConstants.STRING
            )
        check(latestVersionNode is String) {
            """Unexpected node for latest version:
                $latestVersionNode / ${latestVersionNode::class.java}
            """.trimIndent()
        }
        latestVersionNode
    }

    @Before
    fun setup() {
        projectRoot = projectSetup.rootDir
        gradleRunner = GradleRunner.create()
            .withProjectDir(projectRoot)
            .withArguments("clean", "compileDebugKotlin")
        setupProjectBuildGradle()
        setupSettingsGradle()
        setupAndroidManifest()
        addSource()
    }

    @Test
    fun usingNoRuntime() {
        setupAppBuildGradle("") // Not adding any Compose runtime to classpath
        val result = gradleRunner.buildAndFail()
        assertEquals(TaskOutcome.FAILED, result.task(":app:compileDebugKotlin")!!.outcome)
        assertTrue(
            result.output.contains(
                "The Compose Compiler requires the Compose Runtime to be on the class " +
                    "path, but none could be found."
            )
        )
    }

    @Test
    fun usingLatestUnsupportedRuntime() {
        setupAppBuildGradle("""implementation("androidx.compose.runtime:runtime:1.0.0-rc02")""")
        val result = gradleRunner.buildAndFail()
        assertEquals(TaskOutcome.FAILED, result.task(":app:compileDebugKotlin")!!.outcome)
        assertTrue(
            result.output.contains(
                "You are using an outdated version of Compose Runtime that is not " +
                    "compatible with the version of the Compose Compiler plugin you have installed"
            )
        )
    }

    @Test
    fun usingLastStableRuntime() {
        setupAppBuildGradle("""implementation("androidx.compose.runtime:runtime:1.0.0")""")
        val result = gradleRunner.build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":app:compileDebugKotlin")!!.outcome)
    }

    @Test
    fun usingLatestRuntime() {
        setupAppBuildGradle("""implementation("androidx.compose.runtime:runtime:+")""")
        val result = gradleRunner.build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":app:compileDebugKotlin")!!.outcome)
    }

    private fun setupProjectBuildGradle() {
        val kotlinGradlePlugin =
            "org.jetbrains.kotlin:kotlin-gradle-plugin:${projectSetup.props.kotlinVersion}"
        val repositoriesBlock = buildString {
            appendLine("repositories {")
            appendLine("maven { url \"${projectSetup.props.tipOfTreeMavenRepoPath}\" }")
            projectSetup.allRepositoryPaths.forEach {
                appendLine(
                    """
                    maven {
                        url "$it"
                    }
                    """.trimIndent()
                )
            }
            appendLine("}")
        }
        addFileWithContent(
            "build.gradle",
            """
            buildscript {
                $repositoriesBlock
                dependencies {
                    classpath "${projectSetup.props.agpDependency}"
                    classpath "$kotlinGradlePlugin"
                }
            }

            allprojects {
                $repositoriesBlock
            }

            task clean(type: Delete) {
                delete rootProject.buildDir
            }
            """.trimIndent()
        )
    }

    private fun setupAppBuildGradle(dependenciesBlock: String) {
        addFileWithContent(
            "app/build.gradle",
            """
            apply plugin: "com.android.application"
            apply plugin: "kotlin-android"

            android {
                compileSdkVersion ${projectSetup.props.compileSdkVersion}
                buildToolsVersion "${projectSetup.props.buildToolsVersion}"
                defaultConfig {
                    minSdkVersion 21
                }
                signingConfigs {
                    debug {
                        storeFile file("${projectSetup.props.debugKeystore}")
                    }
                }
                buildFeatures {
                    compose true
                }
                composeOptions {
                    kotlinCompilerExtensionVersion "$compilerPluginVersion"
                }
            }

            dependencies {
                $dependenciesBlock
            }
            """.trimIndent()
        )
    }

    private fun setupSettingsGradle() {
        addFileWithContent(
            "settings.gradle",
            """
            include ':app'
            """.trimIndent()
        )
    }

    private fun setupAndroidManifest() {
        addFileWithContent(
            "$MAIN_DIR/AndroidManifest.xml",
            """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="androidx.compose.compiler.test">
            </manifest>
            """.trimIndent()
        )
    }

    private fun addSource() {
        // Need at least one kotlin file so KotlinCompile task runs
        addFileWithContent(
            "$SOURCE_DIR/Test.kt",
            """
            package androidx.compose.compiler.test
            const val number = 5
            """.trimIndent()
        )
    }

    private fun addFileWithContent(relativePath: String, content: String) {
        val file = File(projectRoot, relativePath)
        file.parentFile.mkdirs()
        file.writeText(content)
    }
}