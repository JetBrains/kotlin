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

val outputDir = (findProperty("docsBuildDir") as String?)?.let{ file(it) } ?: layout.buildDirectory.dir("doc").get().asFile
val inputDirPrevious = file(findProperty("docsPreviousVersionsDir") as String? ?: "$outputDir/previous")
val outputDirPartial = outputDir.resolve("partial")
val kotlin_native_root = file("$kotlin_root/kotlin-native").absolutePath
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

    dokkaPublications.html {
        if (isLatest) {
            outputDirectory.set(outputDirPartial.resolve("latest").resolve(moduleDirName))
        } else {
            outputDirectory.set(
                outputDirPartial.resolve("previous").resolve(moduleDirName).resolve(kotlinLanguageVersion)
            )
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