package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.compile.AbstractCompile
import java.io.File
import kotlin.reflect.KProperty

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