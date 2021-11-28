package org.jetbrains.kotlin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.wrapper.Wrapper

open class CustomWrapper : Wrapper() {
    internal lateinit var mainWrapperTask: Wrapper

    val mainWrapperVersion: String
        @Input get() = mainWrapperTask.gradleVersion
    val mainWrapperDistrSha256: String?
        @Optional @Input get() = mainWrapperTask.distributionSha256Sum
}

open class WrappersExtension {
    var projects = mutableListOf<Any>()
    var distributionType = Wrapper.DistributionType.BIN
}

class GradleWrappers : Plugin<Project> {

    override fun apply(project: Project): Unit = with(project) {
        val mainWrapperTask = tasks.findByName("wrapper") as? Wrapper ?: return@with
        val wrappers = extensions.create(
                WrappersExtension::class.java,
                "wrappers",
                WrappersExtension::class.java
        )
        afterEvaluate {
            wrappers.projects.map { file(it) }.forEach {
                tasks.create("${it.name}Wrapper", CustomWrapper::class.java).apply {
                    this.mainWrapperTask = mainWrapperTask
                    jarFile = it.resolve("gradle/wrapper/gradle-wrapper.jar")
                    scriptFile = it.resolve("gradlew")
                    distributionType = wrappers.distributionType
                    mainWrapperTask.dependsOn(this)

                    // Get these parameters from the main wrapper task to support
                    // command line options like --gradle-version.
                    // Gradle doesn't provide access to the values passed in command line
                    // at the configuration phase, so we have to get these values at the execution phase.
                    doFirst {
                        gradleVersion = mainWrapperVersion
                        distributionSha256Sum = mainWrapperDistrSha256
                    }
                }
            }
        }
    }
}
