package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.InputDirectory
import java.io.File

/**
 * A task that copies samples and replaces direct repository URLs with ones provided by the cache-redirector service.
 * This task also adds kotlin compiler repository from the project's gradle.properties file.
 */
open class CopySamples : Copy() {
    @InputDirectory
    var samplesDir: File = project.file("backend.native/tests/samples")

    private fun configureReplacements() {
        from(samplesDir) {
            exclude("**/*.gradle.kts")
            exclude("**/*.gradle")
            exclude("**/gradle.properties")
        }
        from(samplesDir) {
            include("**/*.gradle")
            include("**/*.gradle.kts")
            filter { line ->
                replacements.forEach { (repo, replacement) ->
                    if (line.contains(repo)) {
                        return@filter line.replace(repo, replacement)
                    }
                }
                return@filter line
            }
        }
        from(samplesDir) {
            include("**/gradle.properties")

            val kotlinVersion = project.property("bootstrapKotlinVersion") as? String
                ?: throw IllegalArgumentException("Property bootstrapKotlinVersion should be specified in the root project")
            val kotlinCompilerRepo = project.property("bootstrapKotlinRepo") as? String
                ?: throw IllegalArgumentException("Property bootstrapKotlinRepo should be specified in the root project")

            filter { line ->
                when {
                    line.startsWith("kotlin_version") -> "kotlin_version=$kotlinVersion"
                    line.startsWith("#kotlinCompilerRepo") || line.startsWith("kotlinCompilerRepo") ->
                        "kotlinCompilerRepo=$kotlinCompilerRepo"
                    else -> line
                }
            }
        }
    }

    override fun configure(closure: Closure<Any>): Task {
        super.configure(closure)
        configureReplacements()
        return this
    }

    private val replacements = listOf(
        "https://plugins.gradle.org/m2" to "https://cache-redirector.jetbrains.com/plugins.gradle.org/m2",
        "mavenCentral()" to "maven { setUrl(\"https://cache-redirector.jetbrains.com/maven-central\") }",
    )
}
