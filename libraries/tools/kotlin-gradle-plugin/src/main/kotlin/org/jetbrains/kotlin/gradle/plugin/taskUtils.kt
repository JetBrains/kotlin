package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.file.FileCollection
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.compile.AbstractCompile
import java.io.File
import kotlin.reflect.KProperty

/*
 * Adding fake up-to-date check to do something before execution, and before input snapshots are taken.
 * Note, this hack is sensitive to up-to-date checks order, so it should be added before any custom up-to-date checks
 * that could return false.
 *
 * Should be used only for compileKotlinTask.
 *
 * // todo: could add configureKotlinTask that always executes before compileKotlin instead
 */
internal fun AbstractCompile.updateClasspathBeforeTask(newClassPath: (oldClassPath: FileCollection) -> FileCollection) {
    // Gradle can take inputs snapshots before task run (normally) or after task run (--rerun-tasks or some up-to-date when returned false).
    // Fake up-to-date check to update classpath dynamically before inputs snapshot is taken.
    // Won't be called in case of some other up-to-date check returned false before this one evaluation
    // Won't be called in case of --rerun-tasks
    var classpathIsUpdated = false
    outputs.upToDateWhen {
        classpath = newClassPath(classpath)
        classpathIsUpdated = true
        true
    }

    // in case fake up-to-date check was not called (see comment above)
    doFirst {
        if (!classpathIsUpdated) {
            classpath = newClassPath(classpath)
        }
    }
}

internal fun AbstractCompile.appendClasspathDynamically(file: File) {
    var added = false

    doFirst {
        if (file !in classpath) {
            classpath += project.files(file)
            added = true
        }
    }
    doLast {
        if (added) {
            classpath -= project.files(file)
        }
    }
}

internal var AbstractTask.anyClassesCompiled: Boolean? by TaskPropertyDelegate("anyClassesCompiled")
internal var AbstractTask.kotlinDestinationDir: File? by TaskPropertyDelegate("kotlinDestinationDir")

inline
internal fun <reified T : Any> TaskPropertyDelegate(propertyName: String) =
        TaskPropertyDelegate(propertyName, T::class.java)

internal class TaskPropertyDelegate<T : Any>(private val propertyName: String, private val klass: Class<T>) {
    operator fun getValue(task: Any?, property: KProperty<*>): T? {
        if (task !is AbstractCompile) throw IllegalStateException("TaskPropertyDelegate could extend only AbstractCompile")

        if ( !task.hasProperty(propertyName)) return null

        return task.property(propertyName)?.let { klass.cast(it) }
    }

    operator fun setValue(task: Any?, property: KProperty<*>, value: T?) {
        if (task !is AbstractCompile) throw IllegalStateException("TaskPropertyDelegate could extend only AbstractCompile")

        task.setProperty(propertyName, value)
    }
}