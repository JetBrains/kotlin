package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.Task
import org.gradle.api.tasks.Copy

/**
 * A task that copies samples and replaces direct repository URLs with ones provided by the cache-redirector service.
 */
open class CopySamples: Copy() {

    var samplesDir = project.file("samples")

    init {
        configureReplacements()
    }

    fun samplesDir(path: Any) {
        samplesDir = project.file(path)
    }

    private fun configureReplacements() {
        from(samplesDir) {
            it.exclude("**/*.gradle")
        }
        from(samplesDir) {
            it.include("**/*.gradle")
            it.filter { line ->
                replacements.forEach { (repo, replacement) ->
                    if (line.contains(repo)) {
                        return@filter line.replace(repo, replacement)
                    }
                }
                return@filter line
            }
        }
    }

    override fun configure(closure: Closure<Any>): Task {
        super.configure(closure)
        configureReplacements()
        return this
    }

    companion object {
        val replacements = listOf(
            "mavenCentral()" to "maven { url 'https://cache-redirector.jetbrains.com/maven-central' }",
            "jcenter()" to "maven { url 'https://cache-redirector.jetbrains.com/jcenter' }",
            "https://dl.bintray.com/kotlin/kotlin-dev" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-dev",
            "https://dl.bintray.com/kotlin/kotlin-eap" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-eap",
            "https://dl.bintray.com/kotlin/ktor" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/ktor",
            "https://plugins.gradle.org/m2" to "https://cache-redirector.jetbrains.com/plugins.gradle.org/m2"
        )
    }
}
