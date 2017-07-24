package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Assert
import org.junit.AssumptionViolatedException
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class PluginsDslIT : BaseGradleIT() {

    @Test
    fun testAllopenWithPluginsDsl() {
        val project = projectWithMavenLocalPlugins("allopenPluginsDsl")
        project.build("build") {
            assertSuccessful()
            assertTasksExecuted(listOf(":compileKotlin"))
        }
    }

    @Test
    fun testApplyToSubprojects() {
        val project = projectWithMavenLocalPlugins("applyToSubprojects")
        project.build("build") {
            assertSuccessful()
            assertTasksExecuted(listOf(":subproject:compileKotlin"))
        }
    }

    @Test
    fun testApplyAllPlugins() {
        val project = projectWithMavenLocalPlugins("applyAllPlugins")

        val kotlinPluginClasses = setOf(
                "org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper",
                "org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin",
                "org.jetbrains.kotlin.allopen.gradle.AllOpenGradleSubplugin",
                "org.jetbrains.kotlin.allopen.gradle.SpringGradleSubplugin",
                "org.jetbrains.kotlin.noarg.gradle.NoArgGradleSubplugin",
                "org.jetbrains.kotlin.noarg.gradle.KotlinJpaSubplugin")

        project.build("build") {
            assertSuccessful()
            val appliedPlugins = "applied plugin class\\:(.*)".toRegex().findAll(output).map { it.groupValues[1] }.toSet()
            kotlinPluginClasses.forEach {
                Assert.assertTrue("Plugin class $it should be in applied plugins", it in appliedPlugins)
            }
        }
    }

    companion object {
        private const val GRADLE_VERSION = "4.0"
        private const val DIRECTORY_PREFIX = "pluginsDsl"

        private const val MAVEN_LOCAL_URL_PLACEHOLDER = "<mavenLocalUrl>"
        private const val PLUGIN_MARKER_VERSION_PLACEHOLDER = "<pluginMarkerVersion>"

        // Workaround for the restriction that snapshot versions are not supported
        private val MARKER_VERSION = KOTLIN_VERSION + (System.getProperty("pluginMarkerVersionSuffix") ?: "-test")
    }

    private fun projectWithMavenLocalPlugins(
            projectName: String,
            wrapperVersion: String = GRADLE_VERSION,
            directoryPrefix: String? = DIRECTORY_PREFIX,
            minLogLevel: LogLevel = LogLevel.DEBUG
    ): Project {

        val result = Project(projectName, wrapperVersion, directoryPrefix, minLogLevel)
        result.setupWorkingDir()

        val settingsGradle = File(result.projectDir, "settings.gradle")
        settingsGradle.modify {
            val mavenLocalUrl = MavenLocalUrlProvider.mavenLocalUrl

            it.replace(MAVEN_LOCAL_URL_PLACEHOLDER, mavenLocalUrl).apply {
                if (this == it)
                    throw AssumptionViolatedException("$MAVEN_LOCAL_URL_PLACEHOLDER placeholder not found in settings.gradle")
            }
        }

        result.projectDir.walkTopDown()
                .filter { it.isFile && it.name == "build.gradle" }
                .forEach { buildGradle ->
                    buildGradle.modify { text ->
                        text.replace(PLUGIN_MARKER_VERSION_PLACEHOLDER, MARKER_VERSION)
                    }
                }

        return result
    }
}

/** Copies the logic of Gradle [`mavenLocal()`](https://docs.gradle.org/3.4.1/dsl/org.gradle.api.artifacts.dsl.RepositoryHandler.html#org.gradle.api.artifacts.dsl.RepositoryHandler:mavenLocal())
 */
private object MavenLocalUrlProvider {
    /** The URL that points to the Gradle's mavenLocal() repository. */
    val mavenLocalUrl by lazy {
        val path = propertyMavenLocalRepoPath ?:
                homeSettingsLocalRepoPath ?:
                m2HomeSettingsLocalRepoPath ?:
                defaultM2RepoPath
        File(path).toURI().toString()
    }

    private val homeDir get() = File(System.getProperty("user.home"))

    private fun getLocalRepositoryFromXml(file: File): String? {
        if (!file.isFile)
            return null

        val xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val localRepoNodes = xml.getElementsByTagName("localRepository")

        if (localRepoNodes.length == 0)
            return null

        val content = localRepoNodes.item(0).textContent

        return content.replace("\\$\\{(.*?)\\}".toRegex()) { System.getProperty(it.groupValues[1]) ?: it.value }
    }

    private val propertyMavenLocalRepoPath get() = System.getProperty("maven.repo.local")

    private val homeSettingsLocalRepoPath
        get() = getLocalRepositoryFromXml(File(homeDir, ".m2/settings.xml"))

    private val m2HomeSettingsLocalRepoPath
        get() = System.getProperty("M2_HOME")?.let { getLocalRepositoryFromXml(File(it, "conf/settings.xml")) }

    private val defaultM2RepoPath get() = File(homeDir, ".m2/repository").absolutePath
}