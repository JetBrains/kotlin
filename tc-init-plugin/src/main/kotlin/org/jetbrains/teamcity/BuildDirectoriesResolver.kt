package org.jetbrains.teamcity

import org.gradle.api.Project
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class BuildDirectoriesResolver {
    private val buildDirs = ConcurrentHashMap<String, File>()

    fun init(project: Project) {
        buildDirs[project.path] = project.layout.buildDirectory.asFile.get()
    }

    fun resolve(taskPath: String): File? {
        val projectPath = if (taskPath.lastIndexOf(':') > 0) {
            taskPath.substring(0, taskPath.lastIndexOf(':'))
        } else {
            ":"
        }
        return buildDirs[projectPath]
    }
}
