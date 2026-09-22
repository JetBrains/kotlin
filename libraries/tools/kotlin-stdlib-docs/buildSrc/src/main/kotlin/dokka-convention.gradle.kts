import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
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

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    dokkaPlugin(project(":plugins:dokka-samples-transformer-plugin"))
    dokkaPlugin(project(":plugins:dokka-version-filter-plugin"))
    dokkaPlugin(libs.findLibrary("dokka-versioning").get())
    dokkaPlugin(libs.findLibrary("dokka-playground").get())
}

val kotlinTemplatesDir = (findProperty("templatesDir") as String?)?.let { file(it) } ?: rootProject.file("templates")
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
