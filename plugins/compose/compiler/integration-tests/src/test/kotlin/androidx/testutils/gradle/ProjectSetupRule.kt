/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.testutils.gradle

import java.io.File
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import org.junit.rules.ExternalResource
import org.junit.rules.TemporaryFolder
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Test rule that helps to setup android project in tests that run gradle.
 *
 * It should be used along side with SdkResourceGenerator in your build.gradle file
 */
class ProjectSetupRule(parentFolder: File? = null) : ExternalResource() {
    val testProjectDir = TemporaryFolder(parentFolder)

    val props: ProjectProps by lazy { ProjectProps.load() }

    val rootDir: File
        get() = testProjectDir.root

    val buildFile: File
        get() = File(rootDir, "build.gradle")

    val gradlePropertiesFile: File
        get() = File(rootDir, "gradle.properties")

    /**
     * Combined list of local build repo and remote repositories (prebuilts etc).
     * Local build repo is the first in line to ensure it is prioritized.
     */
    val allRepositoryPaths: List<String> by lazy {
        listOf(props.tipOfTreeMavenRepoPath) + props.repositoryUrls
    }

    /**
     * A `repositories {}` gradle block that contains all default repositories, for inclusion
     * in gradle configurations.
     */
    val repositories: String
        get() = buildString {
            appendLine("repositories {")
            append(defaultRepoLines)
            appendLine("}")
        }

    val defaultRepoLines
        get() = buildString {
            props.repositoryUrls.forEach {
                appendLine("    maven { url '$it' }")
            }
        }

    val androidProject: String
        get() = """
            android {
                compileSdk ${props.compileSdk}
                buildToolsVersion "${props.buildToolsVersion}"

                defaultConfig {
                    minSdkVersion ${props.minSdkVersion}
                }

                signingConfigs {
                    debug {
                        storeFile file("${props.debugKeystore}")
                    }
                }
            }
        """.trimIndent()

    private val defaultBuildGradle: String
        get() = "\n$repositories\n\n$androidProject\n\n"

    fun writeDefaultBuildGradle(prefix: String, suffix: String) {
        buildFile.writeText(prefix)
        buildFile.appendText(defaultBuildGradle)
        buildFile.appendText(suffix)
    }

    override fun apply(base: Statement, description: Description): Statement {
        return testProjectDir.apply(super.apply(base, description), description)
    }

    override fun before() {
        buildFile.createNewFile()
        copyLocalProperties()
        copyLibsVersionsToml()
        writeGradleProperties()
    }

    fun getSdkDirectory(): String {
        val localProperties = File(props.rootProjectPath, "local.properties")
        when {
            localProperties.exists() -> {
                val stream = localProperties.inputStream()
                val properties = Properties()
                properties.load(stream)
                return properties.getProperty("sdk.dir")
            }
            System.getenv("ANDROID_HOME") != null -> {
                return System.getenv("ANDROID_HOME")
            }
            System.getenv("ANDROID_SDK_ROOT") != null -> {
                return System.getenv("ANDROID_SDK_ROOT")
            }
            else -> {
                throw IllegalStateException(
                    "ProjectSetupRule did find local.properties at: $localProperties and " +
                            "neither ANDROID_HOME or ANDROID_SDK_ROOT was set."
                )
            }
        }
    }

    /**
     * Gets the latest version of a published library.
     *
     * Note that the library must have been locally published to locate its latest version, this
     * can be done in test by adding :publish as a test dependency, for example:
     * ```
     * tasks.findByPath("test")
     *   .dependsOn(tasks.findByPath(":room:room-compiler:publish")
     * ```
     *
     * @param path - The library m2 path e.g. "androidx/room/room-compiler"
     */
    fun getLibraryLatestVersionInLocalRepo(path: String): String {
        val metadataFile = File(props.tipOfTreeMavenRepoPath)
            .resolve(path)
            .resolve("maven-metadata.xml")
        check(metadataFile.exists()) {
            "Cannot find room metadata file in ${metadataFile.absolutePath}"
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
        return latestVersionNode
    }

    private fun copyLocalProperties() {
        var foundSdk = false

        val localProperties = File(props.rootProjectPath, "local.properties")
        if (localProperties.exists()) {
            localProperties.copyTo(File(rootDir, "local.properties"), overwrite = true)
            foundSdk = true
        }

        if (System.getenv("ANDROID_HOME") != null) {
            foundSdk = true
        }

        if (System.getenv("ANDROID_SDK_ROOT") != null) {
            foundSdk = true
        }

        if (!foundSdk) {
            throw IllegalStateException(
                "ProjectSetupRule was unable to copy local.properties at: $localProperties and " +
                        "neither ANDROID_HOME or ANDROID_SDK_ROOT was set."
            )
        }
    }

    private fun copyLibsVersionsToml() {
        val toml = File(props.rootProjectPath, "gradle/libs.versions.toml")
        toml.copyTo(File(rootDir, "gradle/libs.versions.toml"), overwrite = true)
    }

    private fun writeGradleProperties() {
        gradlePropertiesFile.writer().use {
            val props = Properties()
            props.setProperty("android.useAndroidX", "true")
            props.store(it, null)
        }
    }
}

// TODO(b/233600239): document the rest of the parameters
data class ProjectProps(
    val compileSdk: String,
    val buildToolsVersion: String,
    val minSdkVersion: String,
    val debugKeystore: String,
    var navigationRuntime: String,
    val kotlinStblib: String,
    val kgpVersion: String,
    val kgpDependency: String,
    val kspVersion: String,
    val rootProjectPath: String,
    val tipOfTreeMavenRepoPath: String,
    val agpDependency: String,
    val repositoryUrls: List<String>,
    // Not available in playground projects.
    val prebuiltsPath: String?,
) {
    companion object {
        private fun Properties.getCanonicalPath(key: String): String {
            return File(getProperty(key)).canonicalPath
        }

        private fun Properties.getOptionalCanonicalPath(key: String): String? {
            return if (containsKey(key)) {
                getCanonicalPath(key)
            } else {
                null
            }
        }

        fun load(): ProjectProps {
            val stream = ProjectSetupRule::class.java.classLoader.getResourceAsStream("sdk.prop")
                ?: throw IllegalStateException("No sdk.prop file found. " +
                                                       "(you probably need to call SdkResourceGenerator.generateForHostTest " +
                                                       "in build.gradle)")
            val properties = Properties()
            properties.load(stream)
            return ProjectProps(
                debugKeystore = properties.getCanonicalPath("debugKeystoreRelativePath"),
                rootProjectPath = properties.getCanonicalPath("rootProjectRelativePath"),
                tipOfTreeMavenRepoPath = properties.getCanonicalPath(
                    "tipOfTreeMavenRepoRelativePath"
                ),
                repositoryUrls = properties.getProperty("repositoryUrls").split(",").map {
                    if (it.startsWith("http")) {
                        it
                    } else {
                        // Convert relative paths back to canonical paths
                        File(it).canonicalPath
                    }
                },
                compileSdk = properties.getProperty("compileSdk"),
                buildToolsVersion = properties.getProperty("buildToolsVersion"),
                minSdkVersion = properties.getProperty("minSdkVersion"),
                navigationRuntime = properties.getProperty("navigationRuntime"),
                kotlinStblib = properties.getProperty("kotlinStdlib"),
                kgpVersion = properties.getProperty("kgpVersion"),
                kgpDependency = "org.jetbrains.kotlin:kotlin-gradle-plugin:" +
                        properties.getProperty("kgpVersion"),
                kspVersion = properties.getProperty("kspVersion"),
                agpDependency = properties.getProperty("agpDependency"),
                prebuiltsPath = properties.getOptionalCanonicalPath("prebuiltsRelativePath"),
            )
        }
    }
}
