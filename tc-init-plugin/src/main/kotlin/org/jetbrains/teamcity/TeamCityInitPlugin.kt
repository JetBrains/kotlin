package org.jetbrains.teamcity

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.testing.AbstractTestTask

class TeamCityInitPlugin : Plugin<Gradle> {
    override fun apply(gradle: Gradle) {
        gradle.rootProject { rootProject: Project ->
            ProjectExtraPropertiesExtender.logBuildPropertiesUsageWarningMessageIfNeeded(gradle, rootProject.logger)

            val buildDirectoriesResolver = BuildDirectoriesResolver()
            val testNameFormat = System.getenv(Constants.TEST_NAME_FORMAT_ENV_VAR)
            val ignoredSuiteFormat = System.getenv(Constants.IGNORED_SUITE_FORMAT_ENV_VAR)

            // Register FlowScope build problem reporting plugin
            rootProject.plugins.apply(TeamcityBuildProblemReportingPlugin::class.java)

            // Register BuildEventsListenerRegistry based compile error and test logging plugins
            rootProject.plugins.apply(CompileTaskErrorOutputPlugin::class.java)
            rootProject.plugins.apply(TestTaskBuildLogSetupPlugin::class.java)

            gradle.allprojects { project: Project ->
                ProjectExtraPropertiesExtender.extend(project)
                buildDirectoriesResolver.init(project)

                project.tasks.withType(AbstractTestTask::class.java).configureEach { task ->
                    TestTaskWrapper(task, buildDirectoriesResolver, testNameFormat, ignoredSuiteFormat).configure()
                }
            }
        }
    }
}
