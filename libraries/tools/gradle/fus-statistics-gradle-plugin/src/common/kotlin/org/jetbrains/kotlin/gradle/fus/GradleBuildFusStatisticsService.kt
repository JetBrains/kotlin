package org.jetbrains.kotlin.gradle.fus


import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.gradle.kotlin.dsl.withType


internal interface UsesGradleBuildFusStatisticsService : Task {
    @get:Internal
    val fusStatisticsBuildService: Property<GradleBuildFusStatisticsService?>
}

internal abstract class GradleBuildFusStatisticsService : GradleBuildFusStatistics,
    BuildService<GradleBuildFusStatisticsService.Parameters>, AutoCloseable {

    interface Parameters : BuildServiceParameters {
        val path: Property<String>
        val statisticsIsEnabled: Property<Boolean>
    }

    override fun close() {
    }

    companion object {
        private const val CUSTOM_LOGGER_ROOT_PATH = "kotlin.session.logger.root.path"
        private const val STATISTICS_FOLDER_NAME = "kotlin-fus"
        private const val STATISTICS_FILE_NAME_PATTERN = "\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{3}(.\\d+)?.profile"

        private const val BUILD_SESSION_SEPARATOR = "BUILD FINISHED"


        private var statisticsIsEnabled: Boolean = true //KT-59629 Wait for user confirmation before start to collect metrics
        private val serviceClass = GradleBuildFusStatisticsService::class.java
        private val serviceName = "${serviceClass.name}_${serviceClass.classLoader.hashCode()}"

        fun registerIfAbsent(project: Project): Provider<GradleBuildFusStatisticsService> {
            project.gradle.sharedServices.registrations.findByName(serviceName)?.let {
                @Suppress("UNCHECKED_CAST")
                return it.service as Provider<GradleBuildFusStatisticsService>
            }

            return if (statisticsIsEnabled) {
                project.gradle.sharedServices.registerIfAbsent(serviceName, serviceClass) {
                    val customPath: String = if (project.rootProject.hasProperty(CUSTOM_LOGGER_ROOT_PATH)) {
                        project.rootProject.property(CUSTOM_LOGGER_ROOT_PATH) as String
                    } else {
                        project.gradle.gradleUserHomeDir.path //fix
                    }
                    it.parameters.path.set(customPath)
                }
            } else {
                project.gradle.sharedServices.registerIfAbsent(serviceName, DummyGradleBuildFusStatisticsService::class.java) {}
                    .map { it as GradleBuildFusStatisticsService }
            }.also { configureTasks(project, it) }
        }

        private fun configureTasks(project: Project, serviceProvider: Provider<GradleBuildFusStatisticsService>) {
            project.tasks.withType<UsesGradleBuildFusStatisticsService>().configureEach { task ->
                task.fusStatisticsBuildService.value(serviceProvider).disallowChanges()
                task.usesService(serviceProvider)
            }
        }
    }
}

internal abstract class DummyGradleBuildFusStatisticsService : GradleBuildFusStatisticsService() {
    override fun reportBoolean(name: String, value: Boolean, subprojectName: String?, weight: Long?): Boolean {
        //do nothing
        return true
    }

    override fun reportNumber(name: String, value: Long, subprojectName: String?, weight: Long?): Boolean {
        //do nothing
        return true
    }

    override fun reportString(name: String, value: String, subprojectName: String?, weight: Long?): Boolean {
        //do nothing
        return true
    }

}