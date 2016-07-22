package org.jetbrains.kotlin.gradle.tasks

import java.io.File

class CompilerPluginOptions {
    private val mutableClasspath = arrayListOf<String>()
    private val mutableArguments = arrayListOf<String>()

    val classpath: List<String>
        get() = mutableClasspath

    val arguments: List<String>
        get() = mutableArguments

    // used in kotlin-gradle-plugin
    @Suppress("unused")
    fun addClasspathEntry(file: File) {
        mutableClasspath.add(file.canonicalPath)
    }

    fun addPluginArgument(pluginId: String, key: String, value: String) {
        mutableArguments.add("plugin:$pluginId:$key=$value")
    }
}
