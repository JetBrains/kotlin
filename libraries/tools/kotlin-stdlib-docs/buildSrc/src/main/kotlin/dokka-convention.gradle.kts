import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.dokka.gradle.engine.parameters.DokkaSourceSetSpec
import org.jetbrains.dokka.gradle.engine.plugins.DokkaPluginParametersBaseSpec
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import java.net.URI
import kotlin.collections.getValue

/**
 * Common conventions for generating documentation with Dokka.
 */

plugins {
    id("org.jetbrains.dokka")
}

val dokka_version: String by project

dependencies {
    dokkaPlugin(project(":plugins:dokka-samples-transformer-plugin"))
    dokkaPlugin(project(":plugins:dokka-version-filter-plugin"))
    dokkaPlugin("org.jetbrains.dokka:versioning-plugin:$dokka_version")
    dokkaPlugin("org.jetbrains.dokka:kotlin-playground-samples-plugin:$dokka_version")
}

val kotlinTemplatesDir = (findProperty("templatesDir") as String?)?.let { file(it) } ?: rootProject.file("templates")
val kotlin_root = rootProject.file("../../../").absoluteFile.invariantSeparatorsPath
version = rootProject.version

dokka {
    dokkaGeneratorIsolation = ProcessIsolation {
        systemProperties = mapOf(
            "org.jetbrains.dokka.analysis.allowKotlinPackage" to "true",
            "dokka.shouldDisplayAllTypesPage" to "true",
            "dokka.shouldDisplaySinceKotlin" to "true",
        )
        maxHeapSize = "6g"
    }
    pluginsConfiguration {
        html {
            mergeImplicitExpectActualDeclarations = true
            templatesDir = kotlinTemplatesDir
        }
        registerBinding(VersionFilterPluginParameters::class, VersionFilterPluginParameters::class)
    }
}
