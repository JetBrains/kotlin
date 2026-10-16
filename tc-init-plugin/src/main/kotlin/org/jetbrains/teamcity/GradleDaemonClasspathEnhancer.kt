package org.jetbrains.teamcity

import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test
import java.io.File
import java.net.URLClassLoader

object GradleDaemonClasspathEnhancer {
    fun enhanceWithTestClasspath(task: AbstractTestTask) {
        val classNamesList = System.getenv(Constants.GRADLE_DAEMON_ENHANCEMENT_CLASSES_ENV_VAR) ?: return
        val classNames = classNamesList.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (classNames.isEmpty()) return

        val classpath: Set<File>? = if (task is Test) {
            task.classpath.files
        } else {
            try {
                val method = task.javaClass.getMethod("getClasspath")
                val fileCollection = method.invoke(task)
                val getFiles = fileCollection.javaClass.getMethod("getFiles")
                @Suppress("UNCHECKED_CAST")
                getFiles.invoke(fileCollection) as? Set<File>
            } catch (ignored: Throwable) {
                null
            }
        }
        if (classpath == null) return

        val classLoader = task.javaClass.classLoader
        if (classLoader is URLClassLoader) {
            val addURLMethod = URLClassLoader::class.java.getDeclaredMethod("addURL", java.net.URL::class.java)
            addURLMethod.isAccessible = true
            for (file in classpath) {
                if (file.exists()) {
                    addURLMethod.invoke(classLoader, file.toURI().toURL())
                }
            }
        }
    }
}
