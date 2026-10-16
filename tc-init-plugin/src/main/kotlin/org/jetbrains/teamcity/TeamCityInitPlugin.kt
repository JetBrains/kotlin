package org.jetbrains.teamcity

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.testing.AbstractTestTask

class TeamCityInitPlugin : Plugin<Gradle> {
    override fun apply(gradle: Gradle) {
        gradle.rootProject { rootProject: Project ->
            ProjectExtraPropertiesExtender.logBuildPropertiesUsageWarningMessageIfNeeded(gradle, rootProject.logger)
            gradle.addListener(ProjectExtraPropertiesExtender(rootProject.logger))

            val buildDirectoriesResolver = BuildDirectoriesResolver()
            val testNameFormat = System.getenv(Constants.TEST_NAME_FORMAT_ENV_VAR)
            val ignoredSuiteFormat = System.getenv(Constants.IGNORED_SUITE_FORMAT_ENV_VAR)

            try {
                val getServicesMethod = gradle.javaClass.getMethod("getServices")
                val services = getServicesMethod.invoke(gradle)
                val getMethod = services.javaClass.getMethod("get", Class::class.java)

                // Register FlowScope build problem reporting plugin if FlowScope is available
                try {
                    val flowScopeClass = Class.forName("org.gradle.api.flow.FlowScope", false, gradle.javaClass.classLoader)
                    getMethod.invoke(services, flowScopeClass)
                    rootProject.plugins.apply(TeamcityBuildProblemReportingPlugin::class.java)
                } catch (ignored: Throwable) {}

                // Register BuildEventsListenerRegistry based compile error and test logging plugins
                try {
                    val buildEventsListenerRegistryClass = Class.forName("org.gradle.build.event.BuildEventsListenerRegistry", false, gradle.javaClass.classLoader)
                    val buildEventsListenerRegistry = getMethod.invoke(services, buildEventsListenerRegistryClass)

                    CompileTaskErrorOutputPlugin(buildEventsListenerRegistry).apply(rootProject)
                    TestTaskBuildLogSetupPlugin(buildEventsListenerRegistry).apply(rootProject)
                } catch (ignored: Throwable) {}
            } catch (ignored: Throwable) {}

            gradle.addListener(AfterEvaluateListener { project: Project ->
                buildDirectoriesResolver.init(project)

                project.tasks.withType(AbstractTestTask::class.java).configureEach { task ->
                    TestTaskWrapper(task, buildDirectoriesResolver, testNameFormat, ignoredSuiteFormat).configure()
                }

                // JUnit platform test import
                project.tasks.configureEach { task ->
                    if (task.name == "junitPlatformTest") {
                        task.doFirst {
                            task.outputs.files.forEach { file ->
                                val message = ServiceMessage(
                                    name = "importData",
                                    attributes = mutableMapOf(
                                        "type" to "junit",
                                        "path" to "${file.absolutePath}/*.xml"
                                    )
                                )
                                println(message.asString())
                            }
                        }
                    }
                }
            })
        }
    }
}
