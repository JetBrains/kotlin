package org.jetbrains.kotlin.gradle.plugin

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.util.ConfigureUtil
import java.io.File

abstract class KonanBuildingTask: KonanTargetableTask() {
    abstract val artifactPath: String
        @Internal get
    abstract val artifact: File
        @OutputFile get
    abstract val outputDir: File
        @Internal get

    abstract val isLibrary: Boolean
        @Internal get

    @Input
    var dumpParameters: Boolean = false

    val konanVersion
        @Input get() = project.konanVersion
    val konanHome
        @Input get() = project.konanHome

    @Nested
    val libraries = KonanLibrariesSpec(project)
}

// TODO: Implement the target properties here.
abstract class KonanBuildingConfig(val configName: String,
                                   val project: Project): Named {

    override fun getName(): String = configName

    fun dumpParameters(value: Boolean) {
        task.dumpParameters = value
    }

    // TODO: Replace with target enum
    fun target(target: String) {
        task.target = target
    }

    // TODO: Replace with a collection for target support
    abstract internal val task: KonanBuildingTask

    fun dependsOn(dependency: Any) = task.dependsOn(dependency)

    fun libraries(closure: Closure<Unit>) = libraries(ConfigureUtil.configureUsing(closure))
    fun libraries(action: Action<KonanLibrariesSpec>) = libraries { action.execute(this) }
    fun libraries(configure: KonanLibrariesSpec.() -> Unit) {
        task.libraries.configure()
        // TODO: May be rework.
        task.libraries.artifacts.forEach {
            dependsOn(it.task)
        }
    }
}