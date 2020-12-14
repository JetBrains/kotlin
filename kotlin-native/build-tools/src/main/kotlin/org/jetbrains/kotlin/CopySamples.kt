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
    var samplesDir: File = project.file("samples")

    private fun configureReplacements() {
        from(samplesDir) {
            it.exclude("**/*.gradle.kts")
            it.exclude("**/*.gradle")
            it.exclude("**/gradle.properties")
        }
        from(samplesDir) {
            it.include("**/*.gradle")
            it.include("**/*.gradle.kts")
            it.filter { line ->
                replacements.forEach { (repo, replacement) ->
                    if (line.contains(repo)) {
                        return@filter line.replace(repo, replacement)
                    }
                }
                return@filter line
            }
        }
        from(samplesDir) {
            it.include("**/gradle.properties")

            val kotlinVersion = project.property("kotlinVersion") as? String
                ?: throw IllegalArgumentException("Property kotlinVersion should be specified in the root project")
            val kotlinCompilerRepo = project.property("kotlinCompilerRepo") as? String
                ?: throw IllegalArgumentException("Property kotlinCompilerRepo should be specified in the root project")

            it.filter { line ->
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
        "https://dl.bintray.com/kotlin/kotlin-dev" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-dev",
        "https://dl.bintray.com/kotlin/kotlin-eap" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-eap",
        "https://dl.bintray.com/kotlin/ktor" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/ktor",
        "https://plugins.gradle.org/m2" to "https://cache-redirector.jetbrains.com/plugins.gradle.org/m2",
        "mavenCentral()" to "maven { setUrl(\"https://cache-redirector.jetbrains.com/maven-central\") }",
        "jcenter()" to "maven { setUrl(\"https://cache-redirector.jetbrains.com/jcenter\") }",
    )
}
