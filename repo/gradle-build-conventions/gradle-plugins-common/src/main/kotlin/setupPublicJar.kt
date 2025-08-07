@file:Suppress("unused")
@file:JvmName("SetupPublicJar")

import org.gradle.api.Project
import org.gradle.api.file.CopySourceSpec
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import java.util.concurrent.Callable

fun Jar.setupPublicJar(
    baseName: String,
    classifier: String = ""
) = setupPublicJar(
    project.provider { baseName },
    project.provider { classifier }
)

fun Jar.setupPublicJar(
    baseName: Provider<String>,
    classifier: Provider<String> = project.provider { "" }
) {
    val buildNumber = project.rootProject.extra["buildNumber"] as String
    this.archiveBaseName.set(baseName)
    this.archiveClassifier.set(classifier)
    manifest.attributes.apply {
        put("Implementation-Vendor", "JetBrains")
        put("Implementation-Title", baseName.get())
        put("Implementation-Version", buildNumber)
    }
}