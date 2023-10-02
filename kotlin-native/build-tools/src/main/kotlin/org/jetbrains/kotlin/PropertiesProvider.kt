package org.jetbrains.kotlin

import org.gradle.api.Project
import java.util.*

class PropertiesProvider(val project: Project) {
    private val localProperties by lazy {
        Properties().apply {
            project.rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use {
                load(it)
            }
        }
    }

    fun findProperty(name: String): Any? =
            project.findProperty(name) ?: localProperties.getProperty(name)

    fun getProperty(name: String): Any =
            findProperty(name) ?: throw IllegalArgumentException("No such property: $name")

    fun hasProperty(name: String): Boolean =
            project.hasProperty(name) || localProperties.containsKey(name)

    val xcodeMajorVersion: String?
        get() = findProperty("xcodeMajorVersion") as String?

    val checkXcodeVersion: Boolean
        get() = findProperty("checkXcodeVersion")?.let {
            it == "true"
        } ?: true
}
