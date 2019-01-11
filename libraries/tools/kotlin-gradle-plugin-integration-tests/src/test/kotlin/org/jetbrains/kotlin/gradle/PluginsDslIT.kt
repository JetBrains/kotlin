package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Assert
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class PluginsDslIT : BaseGradleIT() {

    @Test
    fun testAllopenWithPluginsDsl() {
        val project = projectWithMavenLocalPlugins("allopenPluginsDsl")
        project.build("build") {
            assertSuccessful()
            assertTasksExecuted(":compileKotlin")
        }
    }

    @Test
    fun testApplyToSubprojects() {
        val project = projectWithMavenLocalPlugins("applyToSubprojects")
        project.build("build") {
            assertSuccessful()
            assertTasksExecuted(":subproject:compileKotlin")
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
            "org.jetbrains.kotlin.noarg.gradle.KotlinJpaSubplugin"
        )

        project.build("build") {
            assertSuccessful()
            val appliedPlugins = "applied plugin class\\:(.*)".toRegex().findAll(output).map { it.groupValues[1] }.toSet()
            kotlinPluginClasses.forEach {
                Assert.assertTrue("Plugin class $it should be in applied plugins", it in appliedPlugins)
            }
        }
    }

    companion object {
        private const val DIRECTORY_PREFIX = "pluginsDsl"

        private fun BaseGradleIT.projectWithMavenLocalPlugins(
            projectName: String,
            wrapperVersion: GradleVersionRequired = GradleVersionRequired.None,
            directoryPrefix: String? = DIRECTORY_PREFIX,
            minLogLevel: LogLevel = LogLevel.DEBUG
        ): Project = transformProjectWithPluginsDsl(projectName, wrapperVersion, directoryPrefix, minLogLevel)
    }
}

private const val MAVEN_LOCAL_URL_PLACEHOLDER = "<mavenLocalUrl>"
private const val PLUGIN_MARKER_VERSION_PLACEHOLDER = "<pluginMarkerVersion>"

internal fun BaseGradleIT.transformProjectWithPluginsDsl(
    projectName: String,
    wrapperVersion: GradleVersionRequired = defaultGradleVersion,
    directoryPrefix: String? = null,
    minLogLevel: LogLevel = LogLevel.DEBUG
): BaseGradleIT.Project {

    val result = Project(projectName, wrapperVersion, directoryPrefix, minLogLevel)
    result.setupWorkingDir()

    val settingsGradle = File(result.projectDir, "settings.gradle").takeIf(File::exists)
    settingsGradle?.modify {
        it.replace(MAVEN_LOCAL_URL_PLACEHOLDER, MavenLocalUrlProvider.mavenLocalUrl)
    }

    result.projectDir.walkTopDown()
        .filter { it.isFile && (it.name == "build.gradle" || it.name == "build.gradle.kts") }
        .forEach { buildGradle ->
            buildGradle.modify(::transformBuildScriptWithPluginsDsl)
        }

    return result
}

internal fun transformBuildScriptWithPluginsDsl(buildScriptContent: String): String =
    buildScriptContent.replace(PLUGIN_MARKER_VERSION_PLACEHOLDER, KOTLIN_VERSION)

/** Copies the logic of Gradle [`mavenLocal()`](https://docs.gradle.org/3.4.1/dsl/org.gradle.api.artifacts.dsl.RepositoryHandler.html#org.gradle.api.artifacts.dsl.RepositoryHandler:mavenLocal())
 */
private object MavenLocalUrlProvider {
    /** The URL that points to the Gradle's mavenLocal() repository. */
    val mavenLocalUrl by lazy {
        val path = propertyMavenLocalRepoPath ?: homeSettingsLocalRepoPath ?: m2HomeSettingsLocalRepoPath ?: defaultM2RepoPath
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