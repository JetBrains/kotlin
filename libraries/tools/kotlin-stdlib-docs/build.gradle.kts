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
val dokka_version: String by project

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
}
