import org.gradle.kotlin.dsl.base
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.dokka.gradle.engine.parameters.DokkaPackageOptionsSpec
import org.jetbrains.dokka.gradle.engine.parameters.DokkaSourceSetSpec
import org.jetbrains.dokka.gradle.engine.parameters.KotlinPlatform

plugins {
    base
    `dokka-convention`
}

val kotlin_root = rootProject.file("../../../").absoluteFile.invariantSeparatorsPath
val kotlin_libs: String by project

val outputDir = (findProperty("docsBuildDir") as String?)?.let{ file(it) } ?: rootProject.layout.buildDirectory.dir("doc").get().asFile
val inputDirPrevious = file(findProperty("docsPreviousVersionsDir") as String? ?: "$outputDir/previous")
val outputDirPartial = outputDir.resolve("partial")
val kotlinTemplatesDir = (findProperty("templatesDir") as String?)?.let { file(it) } ?: rootProject.file("templates")

val isLatest = (findProperty("isLatest") as String?)?.toBoolean() ?: true

dokka {
    val kotlinReflectIncludeMd = file("$kotlin_root/libraries/reflect/Module.md")

    val kotlinReflectClasspath = fileTree("$kotlin_libs/kotlin-reflect")

    val kotlinLanguageVersion = version as String

    val moduleDirName = "kotlin-reflect"

    pluginsConfiguration {
        versioning {
            version.set(kotlinLanguageVersion)
            if (isLatest) {
                olderVersionsDir.set(inputDirPrevious.resolve(moduleDirName))
            }
        }
        if (isLatest) {
            register<VersionFilterPluginParameters>("VersionFilterPlugin") {
                targetVersion = kotlinLanguageVersion
            }
        }
    }

    dokkaSourceSets {
        register("jvm") {
            jdkVersion.set(8)
            analysisPlatform.set(KotlinPlatform.JVM)
            classpath.setFrom(kotlinReflectClasspath)

            displayName.set("JVM")
            sourceRoots.from("$kotlin_root/core/reflection.jvm/src")

            skipDeprecated.set(false)
            includes.from(kotlinReflectIncludeMd)
            languageVersion.set(kotlinLanguageVersion)
            enableKotlinStdLibDocumentationLink.set(false)
            perPackageOption("kotlin.reflect.jvm.internal") {
                suppress.set(true)
            }
            sourceLinksFromRoot(this)
        }
    }
    fixIntersectedSourceRootsAndSamples(dokkaSourceSets, "kotlin.reflect")
}

fun DokkaSourceSetSpec.perPackageOption(packageNamePrefix: String, action: Action<in DokkaPackageOptionsSpec>) =
    perPackageOption {
        matchingRegex.set(Regex.escape(packageNamePrefix) + "(\$|\\..*)")
        action(this)
    }

tasks.named("dokkaGeneratePublicationHtml") {
    enabled = false
}