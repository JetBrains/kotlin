@file:Suppress("unused")
@file:JvmName("SetupPublicJar")

import org.gradle.api.provider.Provider
import org.gradle.jvm.tasks.Jar

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
    val buildNumber = project.kotlinBuildProperties.buildNumber
    this.archiveBaseName.set(baseName)
    this.archiveClassifier.set(classifier)
    manifest.attributes.apply {
        put("Implementation-Vendor", "JetBrains")
        put("Implementation-Title", baseName.get())
        put("Implementation-Version", buildNumber.get())
    }
}
