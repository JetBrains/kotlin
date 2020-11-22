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
            val replacements = replacementsWithWrapper { s -> "maven { url '$s' }" }
            it.filter { line ->
                val repo = line.trim()
                replacements[repo]?.let { r -> line.replace(repo, r) } ?: line
            }
        }
        from(samplesDir) {
            it.include("**/*.gradle.kts")
            val replacements = replacementsWithWrapper { s -> "maven(\"$s\")"}
            it.filter { line ->
                val repo = line.trim()
                replacements[repo]?.let { r -> line.replace(repo, r) } ?: line
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

    private fun replacementsWithWrapper(wrap: (String) -> String) =
        urlReplacements.map { entry ->
            Pair(wrap(entry.key), wrap(entry.value))
        }.toMap() + centralReplacements.map { entry ->
            Pair(entry.key, wrap(entry.value))
        }.toMap()

    private val urlReplacements = mapOf(
        "https://dl.bintray.com/kotlin/kotlin-dev" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-dev",
        "https://dl.bintray.com/kotlin/kotlin-eap" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-eap",
        "https://dl.bintray.com/kotlin/ktor" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/ktor",
        "https://plugins.gradle.org/m2" to "https://cache-redirector.jetbrains.com/plugins.gradle.org/m2"
    )

    private val centralReplacements = mapOf(
        "mavenCentral()" to "https://cache-redirector.jetbrains.com/maven-central",
        "jcenter()" to "https://cache-redirector.jetbrains.com/jcenter",
    )
}
