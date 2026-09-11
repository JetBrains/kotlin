import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

plugins {
    base
    `dokka-convention`
}

val isTeamcityBuild = project.hasProperty("teamcity.version") || System.getenv("TEAMCITY_VERSION") != null

// kotlin/libraries/tools/kotlin-stdlib-docs  ->  kotlin
val kotlin_root = rootProject.file("../../../").absoluteFile.invariantSeparatorsPath
val kotlin_libs = layout.buildDirectory.dir("libs").get().asFile.path
extra["kotlin_libs"] = kotlin_libs

val rootProperties = java.util.Properties().apply {
    file(kotlin_root).resolve("gradle.properties").inputStream().use { stream -> load(stream) }
}
val defaultSnapshotVersion: String by rootProperties
val kotlinLanguageVersion: String by rootProperties

val githubRevision = if (isTeamcityBuild) project.property("githubRevision") else "master"
val artifactsVersion = if (isTeamcityBuild) project.property("deployVersion") as String else defaultSnapshotVersion
extra["artifactsVersion"] = artifactsVersion
val artifactsRepo = if (isTeamcityBuild) project.property("kotlinLibsRepo") as String else "$kotlin_root/build/repo"
extra["artifactsRepo"] = artifactsRepo
val dokka_version: String = libs.versions.dokka.get()

println("# Parameters summary:")
println("    isTeamcityBuild: $isTeamcityBuild")
println("    dokka version: $dokka_version")
println("    githubRevision: $githubRevision")
println("    language version: $kotlinLanguageVersion")
println("    artifacts version: $artifactsVersion")
println("    artifacts repo: $artifactsRepo")


val outputDir = (findProperty("docsBuildDir") as String?)?.let{ file(it) } ?: layout.buildDirectory.dir("doc").get().asFile
val inputDirPrevious = file(findProperty("docsPreviousVersionsDir") as String? ?: "$outputDir/previous")
val outputDirPartial = outputDir.resolve("partial")
val kotlin_native_root = file("$kotlin_root/kotlin-native").absolutePath
val templatesDir = file(findProperty("templatesDir") as String? ?: "$projectDir/templates").invariantSeparatorsPath

val cleanDocs = tasks.register("cleanDocs", Delete::class) {
    delete(outputDir)
}

tasks.clean {
    dependsOn(cleanDocs)
}

val prepare = tasks.register("prepare") {
    dependsOn(":kotlin_big:extractLibs")
}

version = (findProperty("version") as String?).takeIf { it != "unspecified"}  ?: kotlinLanguageVersion
val isLatest = (findProperty("isLatest") as String?)?.toBoolean() ?: true


(getTasksByName("dokkaGenerateHtml", true) + getTasksByName("dokkaGenerate", true) + getTasksByName(
    "dokkaGenerateModuleHtml", true
) + getTasksByName("dokkaGeneratePublicationHtml", true)).forEach {
    it.dependsOn(prepare)
}

dependencies {
    dokka(project(":kotlin-stdlib"))
    dokka(project(":kotlin-test"))
    dokka(project(":kotlin-reflect"))
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    }
}

 dokka {
     val moduleDirName = "all-libs"
     pluginsConfiguration {
         versioning {
             version.set(kotlinLanguageVersion)
             if (isLatest) {
                 olderVersionsDir.set(inputDirPrevious.resolve(moduleDirName))
                 // kotlinlang.org serves this output from `/api/core/`, where an
                 // `older/` path segment is stripped by a path-independent rewrite,
                 // so anything published there becomes unreachable and the whole
                 // version selector 404s (KT-89292).
                 olderVersionsDirName.set("")
             }
         }
         if (isLatest) {
             register<VersionFilterPluginParameters>("VersionFilterPlugin") {
                 targetVersion = kotlinLanguageVersion
             }
         }
     }
     moduleName.set("Kotlin libraries")

     dokkaPublications.html {
         if (isLatest) {
             outputDirectory.set(outputDir.resolve("latest").resolve(moduleDirName))
         } else {
             outputDirectory.set(
                 outputDir.resolve("previous").resolve(moduleDirName).resolve(kotlinLanguageVersion)
             )
         }
     }
 }

/// Capture all project state at configuration time to stay configuration-cache compatible
val dokkaOutputDirectory = dokka.dokkaPublications.html.get().outputDirectory.get().asFile
val moduleArtifacts = configurations["dokka"].allDependencies.withType(ProjectDependency::class.java)
    .map { dependency ->
        val dependencyProject = project(dependency.path)
        dependencyProject.layout.buildDirectory.file("dokka-module/html/module-descriptor.json").get().asFile to
                dependencyProject.layout.buildDirectory.file("dokka-module/html/module/package-list").get().asFile
    }
val olderVersionsInputDir = inputDirPrevious.resolve("all-libs")

getTasksByName("dokkaGeneratePublicationHtml", false).forEach { task ->
    // Copy into locals so the doLast closure captures these values, not the enclosing
    // build-script object (which the configuration cache cannot serialize).
    val outputDirectory = dokkaOutputDirectory
    val artifacts = moduleArtifacts
    task.doLast {
        artifacts.forEach { (jsonFile, packageList) ->
            val fileAsJsonObject = Json.decodeFromString<JsonObject>(jsonFile.readText())
            val modulePath = (fileAsJsonObject.get("modulePath") as JsonPrimitive).content
            val targetDir = outputDirectory.resolve(modulePath)
            targetDir.mkdirs()
            packageList.copyTo(targetDir.resolve(packageList.name), overwrite = true)
        }
    }

    if (isLatest) {
        // Locals again: the doLast closure must not capture the build script object.
        val olderVersionsInput = olderVersionsInputDir
        val currentVersion = kotlinLanguageVersion
        val requireOlderVersions = isTeamcityBuild
        task.doLast(Action<Task> {
            // The version id of an older documentation archive comes from the contents of its
            // `version.json`, the directory name is irrelevant for the versioning plugin.
            val expectedVersions = olderVersionsInput.listFiles().orEmpty()
                .filter { it.isDirectory && !it.isHidden }
                .mapNotNull { dir -> dir.resolve("version.json").takeIf { it.isFile } }
                .map { Json.decodeFromString<JsonObject>(it.readText()) }
                .map { (it.get("version") as JsonPrimitive).content }

            val defaultLayoutDir = outputDirectory.resolve("older")
            check(!defaultLayoutDir.exists()) {
                "Dokka versioning has fallen back to its default layout and created " +
                        "$defaultLayoutDir. Older versions must be published at the root " +
                        "of the output, see `olderVersionsDirName` in the `versioning` " +
                        "block (KT-89292)."
            }

            val missingVersions = expectedVersions.filterNot { version ->
                outputDirectory.resolve(version).resolve("version.json").isFile
            }
            check(missingVersions.isEmpty()) {
                "Older documentation versions $missingVersions found in " +
                        "$olderVersionsInput have not been copied into $outputDirectory."
            }

            if (expectedVersions.isEmpty()) {
                val message = "No older documentation versions found in " +
                        "$olderVersionsInput, the published version selector will only " +
                        "offer $currentVersion."
                if (requireOlderVersions) error(message) else logger.warn("w: $message")
            }
        })
    }
}
