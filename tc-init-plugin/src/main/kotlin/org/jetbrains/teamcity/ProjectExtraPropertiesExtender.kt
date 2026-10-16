package org.jetbrains.teamcity

import org.gradle.api.Project
import org.gradle.api.ProjectEvaluationListener
import org.gradle.api.ProjectState
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import java.io.IOException
import java.util.Properties

class ProjectExtraPropertiesExtender(private val logger: Logger) : ProjectEvaluationListener {

    override fun beforeEvaluate(project: Project) {
        addTeamCityPropertiesToExtraProperties(project)
    }

    override fun afterEvaluate(project: Project, projectState: ProjectState) {}

    companion object {
        fun logBuildPropertiesUsageWarningMessageIfNeeded(gradle: Gradle, logger: Logger) {
            if (doNotPopulateGradleProperties() || !BuildFeatures.isConfigurationCacheRequested(gradle)) return

            val warningMessage =
                "\nGradle Configuration Cache is used in this build.\n" +
                        "Avoid accessing TeamCity build properties from the Gradle project " +
                        "('project.teamcity.buildProperty', 'project.buildProperty', 'teamcity.buildProperty', '\$buildProperty', etc.), " +
                        "as they can cause issues in Configuration Cache setups and will be removed for newer Gradle versions.\n" +
                        "If you experience unwarranted Configuration Cache invalidations, " +
                        "disable the population of the project properties with TeamCity build properties by setting " +
                        "the 'teamcity.internal.gradle.runner.doNotPopulateGradleProperties' configuration parameter to 'true'.\n"
            logger.lifecycle(warningMessage)
        }

        fun doNotPopulateGradleProperties(): Boolean {
            return System.getenv(Constants.DO_NOT_POPULATE_GRADLE_PROPERTIES_ENV_VAR)?.toBoolean() == true ||
                    System.getProperty("teamcity.internal.gradle.runner.doNotPopulateGradleProperties")?.toBoolean() == true
        }

        private fun getPropertiesFromFile(logger: Logger): Properties {
            return try {
                PropertiesFileReader.loadPropertiesFromFile()
            } catch (e: IOException) {
                logger.log(LogLevel.ERROR, e.message, e)
                Properties()
            }
        }
    }

    private fun addTeamCityPropertiesToExtraProperties(project: Project) {
        if (doNotPopulateGradleProperties()) return

        val properties = getPropertiesFromFile(logger)
        val ext = project.extensions.extraProperties
        if (!BuildFeatures.isConfigurationCacheRequested(project.gradle)) {
            for ((key, value) in properties) {
                ext.set(key.toString(), value)
            }
            ext.set("teamcity", properties)
        } else {
            val nonVolatileProperties = project.providers.of(TeamCityNonVolatilePropertiesValueSource::class.java) {}.orNull
            nonVolatileProperties?.forEach { (key, value) ->
                ext.set(key.toString(), value)
            }
            val tcExtraPropertiesMap = TeamCityExtraPropertiesMap(project.providers, properties)
            ext.set("teamcity", tcExtraPropertiesMap)
        }
    }
}
